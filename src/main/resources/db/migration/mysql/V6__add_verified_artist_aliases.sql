CREATE TABLE `Artist_Aliases` (
  `kopis_artist_id` BIGINT NOT NULL,
  `spotify_artist_id` VARCHAR(255) NULL,
  `source_type` VARCHAR(50) NULL,
  `external_entity_id` VARCHAR(255) NULL,
  `resolution_status` VARCHAR(50) NOT NULL,
  `last_attempted_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  CONSTRAINT `pk_artist_aliases_id` PRIMARY KEY (`kopis_artist_id`),
  CONSTRAINT `fk_artist_aliases_kopis_artist_id`
    FOREIGN KEY (`kopis_artist_id`) REFERENCES `Artists` (`artist_id`),
  INDEX `idx_artist_aliases_spotify_artist_id` (`spotify_artist_id`),
  INDEX `idx_artist_aliases_resolution_status` (`resolution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
