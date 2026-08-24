CREATE TABLE `Spotify_Artist_Name_Aliases` (
  `name_alias_id` BIGINT NOT NULL AUTO_INCREMENT,
  `artist_id` BIGINT NOT NULL,
  `alias_name` VARCHAR(255) NOT NULL,
  `normalized_alias_name` VARCHAR(255) NOT NULL,
  `language_code` VARCHAR(10) NOT NULL,
  `source_type` VARCHAR(50) NOT NULL,
  `external_entity_id` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  CONSTRAINT `pk_spotify_artist_name_aliases` PRIMARY KEY (`name_alias_id`),
  CONSTRAINT `fk_spotify_artist_name_aliases_artist_id`
    FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`),
  CONSTRAINT `uk_spotify_artist_name_aliases_artist_name`
    UNIQUE (`artist_id`, `normalized_alias_name`),
  INDEX `idx_spotify_artist_name_aliases_artist_id` (`artist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- 기존 동기화 결과도 새 제목 별칭 테이블에 한 번 채우도록 재처리한다.
UPDATE `Spotify_Artist_Alias_Sync_Status`
SET `resolution_status` = 'FAILED';
