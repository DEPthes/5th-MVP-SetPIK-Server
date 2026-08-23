CREATE TABLE `Artist_Genre_Sync_Status` (
  `artist_id` BIGINT NOT NULL,
  `resolution_status` VARCHAR(50) NOT NULL,
  `external_entity_id` VARCHAR(255) NULL,
  `last_attempted_at` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  CONSTRAINT `pk_artist_genre_sync_status_id` PRIMARY KEY (`artist_id`),
  CONSTRAINT `fk_artist_genre_sync_status_artist_id`
    FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`),
  INDEX `idx_artist_genre_sync_resolution_status` (`resolution_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
