package com.hcs.rpgcore.dismantle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.bukkit.inventory.ItemStack;

/**
 * DB 장애 시 분해 GUI의 미반환 장비를 보관하는 파일 저장소.
 *
 * 파일은 RPGCore 플러그인 데이터 폴더 아래에 저장한다.
 */
public final class EquipmentDismantleRecoveryStore {

    private final Path directory;

    public EquipmentDismantleRecoveryStore(
            Path pluginDataDirectory
    ) {
        this.directory =
                pluginDataDirectory.resolve("dismantle-recovery");
    }

    public boolean exists(
            UUID playerId
    ) {
        return Files.exists(fileFor(playerId));
    }

    public void save(
            UUID playerId,
            ItemStack item
    ) throws IOException {

        if (item == null
                || item.getType().isAir()
                || item.getAmount() <= 0) {

            throw new IllegalArgumentException(
                    "복구할 장비가 없습니다."
            );
        }

        Files.createDirectories(directory);

        Path target = fileFor(playerId);

        /*
         * 기존 미수령 장비를 덮어쓰지 않는다.
         */
        if (Files.exists(target)) {
            throw new IOException(
                    "이미 복구 대기 중인 장비가 있습니다: " + playerId
            );
        }

        byte[] data = item.serializeAsBytes();

        Path temporary = Files.createTempFile(
                directory,
                playerId.toString() + "-",
                ".tmp"
        );

        try {

            Files.write(temporary, data);

            /*
             * 같은 디렉터리 내에서 임시 파일을
             * 완성된 복구 파일로 원자적으로 이동한다.
             */
            Files.move(
                    temporary,
                    target,
                    StandardCopyOption.ATOMIC_MOVE
            );

        } finally {

            Files.deleteIfExists(temporary);
        }
    }

    public ItemStack load(
            UUID playerId
    ) throws IOException {

        Path target = fileFor(playerId);

        if (!Files.exists(target)) {
            return null;
        }

        byte[] data = Files.readAllBytes(target);

        if (data.length == 0) {
            throw new IOException(
                    "복구 파일이 비어 있습니다: " + playerId
            );
        }

        try {

            return ItemStack.deserializeBytes(data);

        } catch (RuntimeException exception) {

            throw new IOException(
                    "복구 파일의 장비 데이터를 읽지 못했습니다: "
                            + playerId,
                    exception
            );
        }
    }

    public void delete(
            UUID playerId
    ) throws IOException {

        Files.deleteIfExists(fileFor(playerId));
    }

    private Path fileFor(
            UUID playerId
    ) {
        return directory.resolve(
                playerId.toString() + ".bin"
        );
    }
}
