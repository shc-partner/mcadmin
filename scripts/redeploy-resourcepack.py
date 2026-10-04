#!/usr/bin/env python3

import hashlib
import json
import re
import shutil
import subprocess
import zipfile
from datetime import datetime
from pathlib import Path


# ============================================================
# VERSION / URL
# ============================================================

RESOURCE_PACK_VERSION = "0.4.9"

RESOURCE_PACK_BASE_URL = (
    "http://46.250.248.12:8080"
)


# ============================================================
# 경로 설정
# ============================================================

PACK_DIR = Path(
    "/srv/minecraft/resourcepacks/rpgcore"
)

DEPLOY_ZIP = Path(
    f"/srv/minecraft/resourcepacks/"
    f"rpgcore-{RESOURCE_PACK_VERSION}.zip"
)

TEMP_ZIP = Path(
    "/srv/minecraft/resourcepacks/"
    "rpgcore-redeploy.zip"
)

BETTERMODEL_ZIP = Path(
    "/srv/minecraft/server/plugins/"
    "BetterModel/build.zip"
)

BETTERHUD_ZIP = Path(
    "/srv/minecraft/server/plugins/"
    "BetterHud/build.zip"
)

CRAFTENGINE_ZIP = Path(
    "/srv/minecraft/server/plugins/"
    "CraftEngine/generated/resource_pack.zip"
)

MYTHICARMORS_PACK_DIR = Path(
    "/srv/minecraft/server/plugins/"
    "MythicArmors/pack"
)

MODELENGINE_PACK_DIR = Path(
    "/srv/minecraft/server/plugins/"
    "ModelEngine/resource pack"
)

MYTHICHUD_PACK_DIR = Path(
    "/srv/minecraft/server/plugins/"
    "MythicHUD/built-pack"
)

MERGE_DIR = Path(
    "/srv/minecraft/resourcepacks/"
    ".rpgcore-merge"
)

SERVER_PROPERTIES = Path(
    "/srv/minecraft/server/server.properties"
)

BACKUP_DIR = Path(
    "/srv/minecraft/backups/"
    "resourcepack-redeploy"
)


# ============================================================
# 명령 실행
# ============================================================

def run(
        cmd,
        cwd=None
):

    print()
    print(
        "$",
        " ".join(
            str(x)
            for x in cmd
        )
    )

    subprocess.run(
        [
            str(x)
            for x in cmd
        ],
        cwd=(
            str(cwd)
            if cwd
            else None
        ),
        check=True
    )


# ============================================================
# SHA1 계산
# ============================================================

def calculate_sha1(
        file_path
):

    sha1 = hashlib.sha1()

    with file_path.open("rb") as f:

        while True:

            chunk = f.read(
                1024 * 1024
            )

            if not chunk:
                break

            sha1.update(
                chunk
            )

    return sha1.hexdigest()


# ============================================================
# server.properties용 URL 변환
#
# Minecraft server.properties:
#
# http://46.250.248.12:8080/file.zip
#
# ↓
#
# http\://46.250.248.12\:8080/file.zip
# ============================================================

def escape_properties_url(
        url
):

    return (
        url
        .replace(
            ":",
            r"\:"
        )
    )


# ============================================================
# 기본 파일 확인
# ============================================================

if not PACK_DIR.exists():

    raise SystemExit(
        "ERROR: resource pack "
        f"directory not found: {PACK_DIR}"
    )


if not (
        PACK_DIR
        / "pack.mcmeta"
).exists():

    raise SystemExit(
        "ERROR: pack.mcmeta "
        f"not found: {PACK_DIR}"
    )


if not (
        PACK_DIR
        / "assets"
).exists():

    raise SystemExit(
        "ERROR: assets directory "
        f"not found: {PACK_DIR}"
    )


if not SERVER_PROPERTIES.exists():

    raise SystemExit(
        "ERROR: server.properties "
        f"not found: {SERVER_PROPERTIES}"
    )


# ============================================================
# 시작 정보
# ============================================================

timestamp = datetime.now().strftime(
    "%Y%m%d-%H%M%S"
)

backup = (
    BACKUP_DIR
    / timestamp
)

backup.mkdir(
    parents=True,
    exist_ok=True
)


print(
    "========================================"
)

print(
    " RESOURCE PACK REDEPLOY"
)

print(
    "========================================"
)

print(
    f"VERSION : {RESOURCE_PACK_VERSION}"
)

print(
    f"SOURCE  : {PACK_DIR}"
)

print(
    f"TARGET  : {DEPLOY_ZIP}"
)

print(
    f"BACKUP  : {backup}"
)


# ============================================================
# 백업
# ============================================================

print()
print(
    "=== BACKUP ==="
)


if DEPLOY_ZIP.exists():

    shutil.copy2(
        DEPLOY_ZIP,
        backup
        / DEPLOY_ZIP.name
    )

    print(
        "ZIP backup:"
    )

    print(
        DEPLOY_ZIP
    )


shutil.copy2(
    SERVER_PROPERTIES,
    backup
    / "server.properties"
)

print(
    "server.properties backup:"
)

print(
    SERVER_PROPERTIES
)


# ============================================================
# 임시 ZIP 제거
# ============================================================

if TEMP_ZIP.exists():

    TEMP_ZIP.unlink()


# ============================================================
# ZIP 생성
#
# ZIP ROOT:
#
# pack.mcmeta
# pack.png
# assets/
# ============================================================

print()
print(
    "=== CREATE ZIP ==="
)


# zip_targets는 모든 병합 작업이 끝난 뒤
# MERGE_DIR의 pack.mcmeta를 기준으로 생성한다.


# ============================================================
# RPGCore + BetterHud + BetterModel + ModelEngine + MythicArmors + CraftEngine 병합 디렉터리 생성
# ============================================================

if MERGE_DIR.exists():

    shutil.rmtree(
        MERGE_DIR
    )


shutil.copytree(
    PACK_DIR,
    MERGE_DIR
)



# ============================================================
# BetterHud 최신 리소스 병합
#
# RPGCore의 pack.mcmeta 자체는 유지한다.
# 다만 BetterHud build.zip의:
#
#   1. assets/*
#   2. betterhud_* overlay directories
#   3. pack.mcmeta overlay entries
#
# 를 RPGCore 최종 팩에 병합한다.
#
# BetterHud HUD는 overlay의 text shader에 의존하므로
# assets/*만 복사하면 HUD 이미지가 정상 렌더되지 않는다.
# ============================================================

if not BETTERHUD_ZIP.exists():

    raise SystemExit(
        "ERROR: BetterHud build.zip not found: "
        + str(BETTERHUD_ZIP)
    )


print()
print(
    "=== MERGE BETTERHUD ==="
)

print(
    BETTERHUD_ZIP
)


# ------------------------------------------------------------
# 이전 BetterHud namespace 제거
#
# PACK_DIR에 과거 BetterHud 자산이 남아있는 경우를 대비해
# BetterHud 전용 namespace는 최신 생성본으로 교체한다.
# ------------------------------------------------------------

old_betterhud_assets = (
    MERGE_DIR
    / "assets"
    / "betterhud"
)

if old_betterhud_assets.exists():

    shutil.rmtree(
        old_betterhud_assets
    )

    print(
        "Removed stale BetterHud assets:"
    )

    print(
        old_betterhud_assets
    )


# ------------------------------------------------------------
# build.zip에서 assets/*만 병합
# ------------------------------------------------------------

run(
    [
        "unzip",
        "-q",
        "-o",
        BETTERHUD_ZIP,
        "assets/*",
        "-d",
        MERGE_DIR
    ]
)


# ------------------------------------------------------------
# BetterHud 병합 결과 확인
# ------------------------------------------------------------

betterhud_assets = (
    MERGE_DIR
    / "assets"
    / "betterhud"
)

if not betterhud_assets.exists():

    raise SystemExit(
        "ERROR: BetterHud assets were not merged"
    )


print(
    "BetterHud assets merged successfully"
)


# ============================================================
# BetterHud overlay 병합
# ============================================================

print()
print(
    "=== MERGE BETTERHUD OVERLAYS ==="
)


# ------------------------------------------------------------
# BetterHud build.zip의 pack.mcmeta 읽기
# ------------------------------------------------------------

with zipfile.ZipFile(
        BETTERHUD_ZIP,
        "r"
) as betterhud_archive:

    try:

        betterhud_meta = json.loads(
            betterhud_archive
            .read("pack.mcmeta")
            .decode("utf-8")
        )

    except KeyError:

        raise SystemExit(
            "ERROR: BetterHud pack.mcmeta not found in build.zip"
        )


betterhud_overlay_entries = (
    betterhud_meta
    .get("overlays", {})
    .get("entries", [])
)

if not betterhud_overlay_entries:

    raise SystemExit(
        "ERROR: BetterHud overlay entries not found"
    )


# ------------------------------------------------------------
# 과거 BetterHud overlay 제거
# ------------------------------------------------------------

for old_overlay in MERGE_DIR.glob(
        "betterhud_*"
):

    if old_overlay.is_dir():

        shutil.rmtree(
            old_overlay
        )

        print(
            "Removed stale BetterHud overlay:",
            old_overlay
        )


# ------------------------------------------------------------
# build.zip에서 BetterHud overlay 추출
# ------------------------------------------------------------

for entry in betterhud_overlay_entries:

    directory = entry.get(
        "directory"
    )

    if not directory:

        raise SystemExit(
            "ERROR: BetterHud overlay entry has no directory"
        )

    run(
        [
            "unzip",
            "-q",
            "-o",
            BETTERHUD_ZIP,
            directory + "/*",
            "-d",
            MERGE_DIR
        ]
    )

    target_overlay = (
        MERGE_DIR
        / directory
    )

    if not target_overlay.exists():

        raise SystemExit(
            "ERROR: BetterHud overlay was not extracted: "
            + str(target_overlay)
        )

    print(
        "BetterHud overlay merged:",
        target_overlay
    )


# ------------------------------------------------------------
# RPGCore pack.mcmeta에 BetterHud overlay entries 병합
# ------------------------------------------------------------

merge_meta_path = (
    MERGE_DIR
    / "pack.mcmeta"
)

with merge_meta_path.open(
        "r",
        encoding="utf-8"
) as fp:

    merge_meta = json.load(
        fp
    )


merge_overlays = merge_meta.setdefault(
    "overlays",
    {}
)

merge_entries = merge_overlays.setdefault(
    "entries",
    []
)


# 기존 BetterHud entries 제거
merge_entries[:] = [
    entry
    for entry in merge_entries
    if not str(
        entry.get(
            "directory",
            ""
        )
    ).startswith(
        "betterhud_"
    )
]


# BetterHud entry를 RPGCore pack.mcmeta 형식으로 정규화
for entry in betterhud_overlay_entries:

    directory = entry.get(
        "directory"
    )

    min_format = entry.get(
        "min_format"
    )

    max_format = entry.get(
        "max_format"
    )

    formats = entry.get(
        "formats"
    )

    if min_format is None or max_format is None:

        if (
                isinstance(
                    formats,
                    list
                )
                and len(formats) == 2
        ):

            min_format = formats[0]
            max_format = formats[1]

        elif isinstance(
                formats,
                dict
        ):

            min_format = formats.get(
                "min_inclusive"
            )

            max_format = formats.get(
                "max_inclusive"
            )


    if min_format is None or max_format is None:

        raise SystemExit(
            "ERROR: invalid BetterHud overlay format range: "
            + str(entry)
        )


    normalized_entry = {
        "min_format": int(
            min_format
        ),
        "max_format": int(
            max_format
        ),
        "formats": {
            "min_inclusive": int(
                min_format
            ),
            "max_inclusive": int(
                max_format
            )
        },
        "directory": directory
    }

    merge_entries.append(
        normalized_entry
    )


with merge_meta_path.open(
        "w",
        encoding="utf-8"
) as fp:

    json.dump(
        merge_meta,
        fp,
        ensure_ascii=False,
        indent=2
    )

    fp.write(
        "\n"
    )


print(
    "BetterHud overlays:",
    len(
        betterhud_overlay_entries
    )
)

for entry in betterhud_overlay_entries:

    print(
        " -",
        entry.get(
            "directory"
        )
    )


# ------------------------------------------------------------
# BetterHud 26.x shader 확인
# ------------------------------------------------------------

betterhud_26_overlay = (
    MERGE_DIR
    / "betterhud_26_1"
)

if betterhud_26_overlay.exists():

    betterhud_text_shader = (
        betterhud_26_overlay
        / "assets"
        / "minecraft"
        / "shaders"
        / "core"
        / "rendertype_text.vsh"
    )

    if not betterhud_text_shader.exists():

        raise SystemExit(
            "ERROR: BetterHud 26.x text shader missing: "
            + str(betterhud_text_shader)
        )

    print(
        "BetterHud 26.x text shader verified"
    )



# ------------------------------------------------------------
# 기존 BetterModel 생성 리소스 제거
#
# PACK_DIR에 이전 build.zip의 assets/bettermodel이 남아 있어도
# 최신 BetterModel build.zip 내용으로 완전히 교체한다.
# ------------------------------------------------------------

old_bettermodel_assets = (
    MERGE_DIR
    / "assets"
    / "bettermodel"
)

if old_bettermodel_assets.exists():

    shutil.rmtree(
        old_bettermodel_assets
    )

    print(
        "Removed stale BetterModel assets:"
    )

    print(
        old_bettermodel_assets
    )


# ------------------------------------------------------------
# BetterModel 최신 리소스 병합
#
# build.zip의 pack.mcmeta 등은 사용하지 않고
# assets/만 RPGCore 팩 위에 덮어쓴다.
# ------------------------------------------------------------

if not BETTERMODEL_ZIP.exists():

    raise SystemExit(
        "ERROR: BetterModel build.zip not found: "
        + str(BETTERMODEL_ZIP)
    )


print()
print(
    "=== MERGE BETTERMODEL ==="
)

print(
    BETTERMODEL_ZIP
)


run(
    [
        "unzip",
        "-q",
        "-o",
        BETTERMODEL_ZIP,
        "assets/*",
        "-d",
        MERGE_DIR
    ]
)


# ------------------------------------------------------------
# 병합 결과 확인
# ------------------------------------------------------------

bettermodel_assets = (
    MERGE_DIR
    / "assets"
    / "bettermodel"
)

if not bettermodel_assets.exists():

    raise SystemExit(
        "ERROR: BetterModel assets were not merged"
    )


# ============================================================
# ModelEngine 최신 리소스 병합
# ============================================================

if not MODELENGINE_PACK_DIR.exists():

    raise SystemExit(
        "ERROR: ModelEngine resource pack directory "
        "not found: "
        + str(MODELENGINE_PACK_DIR)
    )


modelengine_pack_mcmeta = (
    MODELENGINE_PACK_DIR
    / "pack.mcmeta"
)

if not modelengine_pack_mcmeta.exists():

    raise SystemExit(
        "ERROR: ModelEngine pack.mcmeta not found: "
        + str(modelengine_pack_mcmeta)
    )


print()
print(
    "=== MERGE MODELENGINE ==="
)

print(
    MODELENGINE_PACK_DIR
)


# ------------------------------------------------------------
# 이전 ModelEngine 생성 namespace 제거
#
# RPGCore 자체 minecraft namespace는 제거하지 않는다.
# assets/modelengine만 최신 생성본으로 교체한다.
# ------------------------------------------------------------

old_modelengine_assets = (
    MERGE_DIR
    / "assets"
    / "modelengine"
)

if old_modelengine_assets.exists():

    shutil.rmtree(
        old_modelengine_assets
    )

    print(
        "Removed stale ModelEngine assets:"
    )

    print(
        old_modelengine_assets
    )


# ------------------------------------------------------------
# 기존 ModelEngine overlay 유지
#
# ModelEngine 생성 pack은 pack.mcmeta에 overlay를 선언해도
# 실제 overlay 디렉터리를 생성하지 않을 수 있다.
# RPGCore source에 이미 존재하는 overlay를 보존하고,
# 생성 pack에 실제 존재하는 것만 아래에서 갱신한다.
# ------------------------------------------------------------


# ------------------------------------------------------------
# ModelEngine assets 병합
#
# assets/modelengine:
#   ModelEngine 모델/텍스처를 최신 생성본으로 교체
#
# assets/minecraft:
#   ModelEngine이 생성한 atlas/item 등의 공용 파일을
#   기존 RPGCore minecraft namespace 위에 병합
# ------------------------------------------------------------

modelengine_assets_source = (
    MODELENGINE_PACK_DIR
    / "assets"
)

modelengine_assets_target = (
    MERGE_DIR
    / "assets"
)

if not modelengine_assets_source.exists():

    raise SystemExit(
        "ERROR: ModelEngine assets not found: "
        + str(modelengine_assets_source)
    )


# ------------------------------------------------------------
# 기존 RPGCore minecraft atlas 보존
# ------------------------------------------------------------

rpgcore_blocks_atlas_path = (
    modelengine_assets_target
    / "minecraft"
    / "atlases"
    / "blocks.json"
)

preserved_rpgcore_atlas_sources = []

if rpgcore_blocks_atlas_path.exists():

    preserved_rpgcore_atlas_data = json.loads(
        rpgcore_blocks_atlas_path.read_text(
            encoding="utf-8"
        )
    )

    preserved_rpgcore_atlas_sources = (
        preserved_rpgcore_atlas_data.get(
            "sources",
            []
        )
    )


shutil.copytree(
    modelengine_assets_source,
    modelengine_assets_target,
    dirs_exist_ok=True
)


# ------------------------------------------------------------
# ModelEngine 병합 후 기존 RPGCore atlas sources 복원
# ------------------------------------------------------------

if (
    preserved_rpgcore_atlas_sources
    and rpgcore_blocks_atlas_path.exists()
):

    merged_atlas_data = json.loads(
        rpgcore_blocks_atlas_path.read_text(
            encoding="utf-8"
        )
    )

    merged_sources = merged_atlas_data.setdefault(
        "sources",
        []
    )

    for source in preserved_rpgcore_atlas_sources:

        if source in merged_sources:
            continue

        resource = source.get("resource")
        sprite = source.get("sprite")

        for existing in merged_sources:

            if (
                resource
                and existing.get("resource") == resource
                and existing != source
            ):
                raise SystemExit(
                    "ERROR: atlas resource collision: "
                    + str(resource)
                )

            if (
                sprite
                and existing.get("sprite") == sprite
                and existing != source
            ):
                raise SystemExit(
                    "ERROR: atlas sprite collision: "
                    + str(sprite)
                )

        merged_sources.append(
            source
        )

    rpgcore_blocks_atlas_path.write_text(
        json.dumps(
            merged_atlas_data,
            ensure_ascii=False,
            separators=(",", ":")
        ),
        encoding="utf-8"
    )

    print(
        "Preserved RPGCore minecraft atlas sources:",
        len(preserved_rpgcore_atlas_sources)
    )


# ------------------------------------------------------------
# ModelEngine pack.mcmeta 읽기
# ------------------------------------------------------------

modelengine_meta = json.loads(
    modelengine_pack_mcmeta.read_text(
        encoding="utf-8"
    )
)

modelengine_overlay_entries = (
    modelengine_meta
    .get("overlays", {})
    .get("entries", [])
)


# ------------------------------------------------------------
# ModelEngine overlay 디렉터리 병합
# ------------------------------------------------------------

for entry in modelengine_overlay_entries:

    directory = entry.get(
        "directory"
    )

    if not directory:
        continue

    source_overlay = (
        MODELENGINE_PACK_DIR
        / directory
    )

    target_overlay = (
        MERGE_DIR
        / directory
    )

    if not source_overlay.exists():

        print(
            "ModelEngine overlay not generated; "
            "keeping existing overlay:"
        )

        print(
            target_overlay
        )

        continue

    shutil.copytree(
        source_overlay,
        target_overlay,
        dirs_exist_ok=True
    )

    print(
        "Updated ModelEngine overlay:"
    )

    print(
        target_overlay
    )


# ------------------------------------------------------------
# MERGE_DIR pack.mcmeta의 ModelEngine overlay 갱신
#
# RPGCore pack 자체의 pack format은 유지한다.
# ModelEngine pack의 "pack" 값으로 덮어쓰지 않는다.
# ------------------------------------------------------------

modelengine_merge_mcmeta_path = (
    MERGE_DIR
    / "pack.mcmeta"
)

modelengine_merge_meta = json.loads(
    modelengine_merge_mcmeta_path.read_text(
        encoding="utf-8"
    )
)

modelengine_merge_overlays = (
    modelengine_merge_meta.setdefault(
        "overlays",
        {}
    )
)

modelengine_merge_entries = (
    modelengine_merge_overlays.setdefault(
        "entries",
        []
    )
)


# 기존 modelengine_* 정의는 모두 제거한 뒤
# 현재 ModelEngine 생성 pack의 정의로 다시 추가한다.

modelengine_merge_entries[:] = [
    entry
    for entry in modelengine_merge_entries
    if not str(
        entry.get(
            "directory",
            ""
        )
    ).startswith(
        "modelengine_"
    )
]

modelengine_merge_entries.extend(
    modelengine_overlay_entries
)


# ModelEngine sodium 설정이 존재하면 최신값 반영

if "sodium" in modelengine_meta:

    modelengine_merge_meta["sodium"] = (
        modelengine_meta["sodium"]
    )


modelengine_merge_mcmeta_path.write_text(
    json.dumps(
        modelengine_merge_meta,
        indent=2,
        ensure_ascii=False
    )
    + "\n",
    encoding="utf-8"
)


# ------------------------------------------------------------
# ModelEngine 병합 검증
# ------------------------------------------------------------

if not (
        MERGE_DIR
        / "assets"
        / "modelengine"
).exists():

    raise SystemExit(
        "ERROR: ModelEngine assets were not merged"
    )


print(
    "ModelEngine assets merged successfully"
)

print(
    "ModelEngine overlays:",
    len(
        modelengine_overlay_entries
    )
)


# ============================================================
# MythicArmors 최신 리소스 병합
# ============================================================

if not MYTHICARMORS_PACK_DIR.exists():

    raise SystemExit(
        "ERROR: MythicArmors pack directory not found: "
        + str(MYTHICARMORS_PACK_DIR)
    )


mythicarmors_pack_mcmeta = (
    MYTHICARMORS_PACK_DIR
    / "pack.mcmeta"
)

if not mythicarmors_pack_mcmeta.exists():

    raise SystemExit(
        "ERROR: MythicArmors pack.mcmeta not found: "
        + str(mythicarmors_pack_mcmeta)
    )


print()
print(
    "=== MERGE MYTHICARMORS ==="
)

print(
    MYTHICARMORS_PACK_DIR
)


# ------------------------------------------------------------
# 이전 MythicArmors 생성 리소스 제거
# ------------------------------------------------------------

old_mythicarmor_assets = (
    MERGE_DIR
    / "assets"
    / "mythicarmor"
)

if old_mythicarmor_assets.exists():

    shutil.rmtree(
        old_mythicarmor_assets
    )


old_mythicarmor_emf = (
    MERGE_DIR
    / "assets"
    / "minecraft"
    / "emf"
)

if old_mythicarmor_emf.exists():

    shutil.rmtree(
        old_mythicarmor_emf
    )


for old_overlay in (
        MERGE_DIR.glob(
            "mythicarmors_*"
        )
):

    if old_overlay.is_dir():

        shutil.rmtree(
            old_overlay
        )


# ------------------------------------------------------------
# MythicArmors assets 병합
# ------------------------------------------------------------

mythicarmor_assets_source = (
    MYTHICARMORS_PACK_DIR
    / "assets"
    / "mythicarmor"
)

mythicarmor_assets_target = (
    MERGE_DIR
    / "assets"
    / "mythicarmor"
)

if not mythicarmor_assets_source.exists():

    raise SystemExit(
        "ERROR: MythicArmors assets not found: "
        + str(mythicarmor_assets_source)
    )


shutil.copytree(
    mythicarmor_assets_source,
    mythicarmor_assets_target
)


mythicarmor_emf_source = (
    MYTHICARMORS_PACK_DIR
    / "assets"
    / "minecraft"
    / "emf"
)

if mythicarmor_emf_source.exists():

    mythicarmor_emf_target = (
        MERGE_DIR
        / "assets"
        / "minecraft"
        / "emf"
    )

    mythicarmor_emf_target.parent.mkdir(
        parents=True,
        exist_ok=True
    )

    shutil.copytree(
        mythicarmor_emf_source,
        mythicarmor_emf_target
    )


# ------------------------------------------------------------
# MythicArmors overlay 병합
# ------------------------------------------------------------

mythicarmors_meta = json.loads(
    mythicarmors_pack_mcmeta.read_text(
        encoding="utf-8"
    )
)

mythicarmors_overlay_entries = (
    mythicarmors_meta
    .get("overlays", {})
    .get("entries", [])
)


for entry in mythicarmors_overlay_entries:

    directory = entry.get(
        "directory"
    )

    if not directory:
        continue

    source_overlay = (
        MYTHICARMORS_PACK_DIR
        / directory
    )

    target_overlay = (
        MERGE_DIR
        / directory
    )

    if not source_overlay.exists():

        raise SystemExit(
            "ERROR: MythicArmors overlay not found: "
            + str(source_overlay)
        )

    shutil.copytree(
        source_overlay,
        target_overlay
    )


# ------------------------------------------------------------
# MERGE_DIR pack.mcmeta에 MythicArmors overlay 반영
# ------------------------------------------------------------

merge_mcmeta_path = (
    MERGE_DIR
    / "pack.mcmeta"
)

merge_meta = json.loads(
    merge_mcmeta_path.read_text(
        encoding="utf-8"
    )
)

merge_overlays = merge_meta.setdefault(
    "overlays",
    {}
)

merge_entries = merge_overlays.setdefault(
    "entries",
    []
)


mythicarmor_directories = {
    entry.get("directory")
    for entry in mythicarmors_overlay_entries
    if entry.get("directory")
}


merge_entries[:] = [
    entry
    for entry in merge_entries
    if entry.get("directory")
    not in mythicarmor_directories
]


merge_entries.extend(
    mythicarmors_overlay_entries
)


merge_mcmeta_path.write_text(
    json.dumps(
        merge_meta,
        indent=2,
        ensure_ascii=False
    )
    + "\n",
    encoding="utf-8"
)


# ------------------------------------------------------------
# MythicArmors 병합 검증
# ------------------------------------------------------------

if not (
        MERGE_DIR
        / "assets"
        / "mythicarmor"
).exists():

    raise SystemExit(
        "ERROR: MythicArmors assets were not merged"
    )


# ============================================================
# CraftEngine 최신 리소스 병합
#
# CraftEngine 자체 pack.mcmeta / pack.png는 사용하지 않는다.
#
# 기존 RPGCore / BetterHud / BetterModel / ModelEngine /
# MythicArmors에서 이미 생성된 파일과 동일 경로가 존재하면
# 덮어쓰지 않고 즉시 중단한다.
# ============================================================

if not CRAFTENGINE_ZIP.exists():

    raise SystemExit(
        "ERROR: CraftEngine resource pack ZIP not found: "
        + str(CRAFTENGINE_ZIP)
    )


print()
print(
    "=== MERGE CRAFTENGINE ==="
)

print(
    CRAFTENGINE_ZIP
)


craftengine_ignored_entries = {
    "pack.mcmeta",
    "pack.png",
}


craftengine_files = []


with zipfile.ZipFile(
        CRAFTENGINE_ZIP,
        "r"
) as craftengine_archive:

    for info in craftengine_archive.infolist():

        name = info.filename.replace(
            "\\",
            "/"
        )

        if name.startswith("./"):

            name = name[2:]

        if not name:
            continue

        if name.endswith("/"):
            continue

        if name in craftengine_ignored_entries:
            continue

        craftengine_files.append(
            (
                info,
                name
            )
        )


if not craftengine_files:

    raise SystemExit(
        "ERROR: CraftEngine ZIP has no mergeable files"
    )


# ------------------------------------------------------------
# CraftEngine 경로 충돌 검사
# ------------------------------------------------------------

craftengine_conflicts = []


for info, name in craftengine_files:

    target = (
        MERGE_DIR
        / name
    )

    if target.exists():

        craftengine_conflicts.append(
            name
        )


if craftengine_conflicts:

    print()
    print(
        "ERROR: CraftEngine resource conflicts detected:"
    )

    for name in craftengine_conflicts:

        print(
            " -",
            name
        )

    raise SystemExit(
        "ERROR: CraftEngine merge aborted because "
        + str(
            len(
                craftengine_conflicts
            )
        )
        + " conflicting file(s) already exist"
    )


# ------------------------------------------------------------
# CraftEngine 파일 병합
# ------------------------------------------------------------

with zipfile.ZipFile(
        CRAFTENGINE_ZIP,
        "r"
) as craftengine_archive:

    for info, name in craftengine_files:

        target = (
            MERGE_DIR
            / name
        )

        target.parent.mkdir(
            parents=True,
            exist_ok=True
        )

        with craftengine_archive.open(
                info,
                "r"
        ) as source:

            with target.open(
                    "wb"
            ) as destination:

                shutil.copyfileobj(
                    source,
                    destination
                )


print(
    "CraftEngine files merged:",
    len(
        craftengine_files
    )
)


if not (
        MERGE_DIR
        / "assets"
).exists():

    raise SystemExit(
        "ERROR: assets directory missing "
        "after CraftEngine merge"
    )


# ============================================================
# ============================================================
# MythicHUD disabled
# ============================================================
#
# MythicHUD JAR is intentionally disabled.
# Do not merge stale MythicHUD built-pack assets or overlays.
#

# FINAL BetterHud verification
# ------------------------------------------------------------

if not (
        MERGE_DIR
        / "assets"
        / "betterhud"
).exists():

    raise SystemExit(
        "ERROR: BetterHud assets missing before final ZIP"
    )


print(
    "BetterHud final verification OK"
)


# ============================================================
# 최종 ZIP 대상 생성
#
# pack.mcmeta의 overlay directory도 ZIP root에 포함한다.
# ============================================================

zip_targets = [
    "pack.mcmeta"
]


if (
        MERGE_DIR
        / "pack.png"
).exists():

    zip_targets.append(
        "pack.png"
    )


zip_targets.append(
    "assets"
)


final_meta = json.loads(
    (
        MERGE_DIR
        / "pack.mcmeta"
    ).read_text(
        encoding="utf-8"
    )
)


for entry in (
        final_meta
        .get("overlays", {})
        .get("entries", [])
):

    directory = entry.get(
        "directory"
    )

    if not directory:
        continue

    overlay_path = (
        MERGE_DIR
        / directory
    )

    if overlay_path.exists():

        if directory not in zip_targets:

            zip_targets.append(
                directory
            )


print()
print(
    "ZIP targets:"
)

for target in zip_targets:

    print(
        " -",
        target
    )


# ============================================================
# 최종 ZIP 생성
# ============================================================

run(
    [
        "zip",
        "-q",
        "-r",
        TEMP_ZIP,
        *zip_targets
    ],
    cwd=MERGE_DIR
)


if not TEMP_ZIP.exists():

    raise SystemExit(
        "ERROR: ZIP creation failed"
    )


# ============================================================
# ZIP 구조 검사
# ============================================================

print()
print(
    "=== VERIFY ZIP ==="
)


result = subprocess.check_output(
    [
        "unzip",
        "-Z1",
        str(
            TEMP_ZIP
        )
    ],
    text=True
)


entries = result.splitlines()


if "pack.mcmeta" not in entries:

    raise SystemExit(
        "ERROR: pack.mcmeta "
        "is not at ZIP root"
    )


if not any(
        entry.startswith(
            "assets/"
        )
        for entry in entries
):

    raise SystemExit(
        "ERROR: assets/ "
        "not found in ZIP"
    )


if (
        PACK_DIR
        / "pack.png"
).exists():

    if "pack.png" not in entries:

        raise SystemExit(
            "ERROR: pack.png "
            "is not at ZIP root"
        )


print(
    "ZIP structure OK"
)


# ============================================================
# 실제 배포 ZIP 교체
# ============================================================

print()
print(
    "=== DEPLOY ZIP ==="
)


shutil.copy2(
    TEMP_ZIP,
    DEPLOY_ZIP
)


print(
    TEMP_ZIP
)

print(
    " ->"
)

print(
    DEPLOY_ZIP
)


# ============================================================
# SHA1 계산
# ============================================================

print()
print(
    "=== SHA1 ==="
)


digest = calculate_sha1(
    DEPLOY_ZIP
)


print(
    digest
)


# ============================================================
# resource-pack URL 생성
# ============================================================

resource_pack_url = (
    f"{RESOURCE_PACK_BASE_URL}/"
    f"{DEPLOY_ZIP.name}"
)


resource_pack_property_url = (
    escape_properties_url(
        resource_pack_url
    )
)


print()
print(
    "=== RESOURCE PACK URL ==="
)

print(
    resource_pack_url
)


# ============================================================
# server.properties 수정
# ============================================================

print()
print(
    "=== UPDATE server.properties ==="
)


text = SERVER_PROPERTIES.read_text(
    encoding="utf-8"
)


# ------------------------------------------------------------
# resource-pack URL
# ------------------------------------------------------------

new_text, url_count = re.subn(
    r"^resource-pack=.*$",
    lambda match:
            "resource-pack="
            + resource_pack_property_url,
    text,
    flags=re.MULTILINE
)


if url_count == 0:

    if not new_text.endswith(
            "\n"
    ):

        new_text += "\n"

    new_text += (
        "resource-pack="
        + resource_pack_property_url
        + "\n"
    )


elif url_count > 1:

    raise SystemExit(
        "ERROR: multiple "
        "resource-pack entries found"
    )


# ------------------------------------------------------------
# resource-pack-sha1
# ------------------------------------------------------------

new_text, sha1_count = re.subn(
    r"^resource-pack-sha1=.*$",
    lambda match:
            "resource-pack-sha1="
            + digest,
    new_text,
    flags=re.MULTILINE
)


if sha1_count == 0:

    if not new_text.endswith(
            "\n"
    ):

        new_text += "\n"

    new_text += (
        "resource-pack-sha1="
        + digest
        + "\n"
    )


elif sha1_count > 1:

    raise SystemExit(
        "ERROR: multiple "
        "resource-pack-sha1 "
        "entries found"
    )


SERVER_PROPERTIES.write_text(
    new_text,
    encoding="utf-8"
)


# ============================================================
# 최종 검증
# ============================================================

print()
print(
    "=== FINAL VERIFY ==="
)


verify_text = (
    SERVER_PROPERTIES
    .read_text(
        encoding="utf-8"
    )
)


# ------------------------------------------------------------
# URL 검증
# ------------------------------------------------------------

url_match = re.search(
    r"^resource-pack=(.*)$",
    verify_text,
    flags=re.MULTILINE
)


if not url_match:

    raise SystemExit(
        "ERROR: resource-pack "
        "URL verification failed"
    )


configured_url = (
    url_match
    .group(1)
    .strip()
)


if (
        configured_url
        != resource_pack_property_url
):

    raise SystemExit(
        "ERROR: configured "
        "resource-pack URL differs"
    )


# ------------------------------------------------------------
# SHA1 검증
# ------------------------------------------------------------

sha1_match = re.search(
    r"^resource-pack-sha1=(.*)$",
    verify_text,
    flags=re.MULTILINE
)


if not sha1_match:

    raise SystemExit(
        "ERROR: SHA1 "
        "verification failed"
    )


configured_sha1 = (
    sha1_match
    .group(1)
    .strip()
)


if configured_sha1 != digest:

    raise SystemExit(
        "ERROR: ZIP SHA1 and "
        "server.properties SHA1 differ"
    )


# ------------------------------------------------------------
# 배포 ZIP 자체 SHA1 재검증
# ------------------------------------------------------------

final_zip_sha1 = (
    calculate_sha1(
        DEPLOY_ZIP
    )
)


if final_zip_sha1 != digest:

    raise SystemExit(
        "ERROR: deployed ZIP "
        "SHA1 changed unexpectedly"
    )


print(
    "resource-pack URL OK"
)

print(
    "resource-pack SHA1 OK"
)

print(
    "deployed ZIP SHA1 OK"
)


# ============================================================
# 결과
# ============================================================

size = (
    DEPLOY_ZIP
    .stat()
    .st_size
)


print()

print(
    "========================================"
)

print(
    " REDEPLOY COMPLETE"
)

print(
    "========================================"
)

print(
    f"VERSION : {RESOURCE_PACK_VERSION}"
)

print(
    f"ZIP     : {DEPLOY_ZIP}"
)

print(
    f"SIZE    : {size:,} bytes"
)

print(
    f"SHA1    : {digest}"
)

print(
    f"URL     : {resource_pack_url}"
)

print(
    f"BACKUP  : {backup}"
)

print()

print(
    "Next:"
)

print(
    "sudo systemctl restart minecraft"
)

print(
    "========================================"
)
