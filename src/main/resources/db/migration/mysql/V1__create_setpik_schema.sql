-- Flyway: V1__create_setpik_schema.sql
-- MySQL 8.0+

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

CREATE TABLE `Users` (
  `user_id` BIGINT NOT NULL AUTO_INCREMENT,
  `status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `last_login_at` DATETIME NULL,
  CONSTRAINT `pk_users_id` PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Auth_Refresh_Tokens` (
  `refresh_token_id` BIGINT NOT NULL AUTO_INCREMENT,
  `token_hash` VARCHAR(255) NOT NULL,
  `expires_at` DATETIME NOT NULL,
  `revoked_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL,
  `last_used_at` DATETIME NULL,
  `user_id` BIGINT NOT NULL,
  CONSTRAINT `pk_auth_refresh_tokens_id` PRIMARY KEY (`refresh_token_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Spotify_accounts` (
  `spotify_account_id` BIGINT NOT NULL AUTO_INCREMENT,
  `spotify_user_id` VARCHAR(255) NOT NULL,
  `spotify_email` VARCHAR(255) NULL,
  `display_name` VARCHAR(255) NULL,
  `profile_image_url` VARCHAR(2048) NULL,
  `access_token_encrypted` TEXT NULL,
  `refresh_token_encrypted` TEXT NULL,
  `token_expires_at` DATETIME NULL,
  `connection_status` VARCHAR(50) NOT NULL,
  `connected_at` DATETIME NOT NULL,
  `disconnected_at` DATETIME NULL,
  `last_profile_synced_at` DATETIME NULL,
  `user_id` BIGINT NOT NULL,
  CONSTRAINT `pk_spotify_accounts_id` PRIMARY KEY (`spotify_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Spotify_account_scopes` (
  `scope_name` VARCHAR(255) NOT NULL,
  `spotify_account_id` BIGINT NOT NULL,
  `is_granted` BOOLEAN NOT NULL,
  `granted_at` DATETIME NOT NULL,
  `revoked_at` DATETIME NULL,
  CONSTRAINT `pk_spotify_account_scopes_id` PRIMARY KEY (`scope_name`, `spotify_account_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Artists` (
  `artist_id` BIGINT NOT NULL AUTO_INCREMENT,
  `spotify_artist_id` VARCHAR(255) NULL,
  `kopis_artist_id` VARCHAR(255) NULL,
  `artist_name` VARCHAR(255) NOT NULL,
  `normalized_name` VARCHAR(255) NOT NULL,
  `image_url` VARCHAR(2048) NULL,
  `spotify_artist_url` VARCHAR(2048) NULL,
  `popularity` SMALLINT NULL,
  `spotify_available` BOOLEAN NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  CONSTRAINT `pk_artists_id` PRIMARY KEY (`artist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Genres` (
  `genre_id` BIGINT NOT NULL AUTO_INCREMENT,
  `genre_name` VARCHAR(255) NOT NULL,
  `normalized_name` VARCHAR(255) NOT NULL,
  `created_at` DATETIME NOT NULL,
  CONSTRAINT `pk_genres_id` PRIMARY KEY (`genre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Artists_Genres` (
  `artist_id` BIGINT NOT NULL,
  `genre_id` BIGINT NOT NULL,
  `source_type` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL,
  CONSTRAINT `pk_artists_genres_id` PRIMARY KEY (`artist_id`, `genre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Spotify_Playlists` (
  `playlist_id` BIGINT NOT NULL AUTO_INCREMENT,
  `spotify_playlist_id` VARCHAR(255) NOT NULL,
  `playlist_name` VARCHAR(255) NOT NULL,
  `description` TEXT NULL,
  `cover_image_url` VARCHAR(2048) NULL,
  `is_public` BOOLEAN NULL,
  `owner_spotify_user_id` VARCHAR(255) NULL,
  `snapshot_id` VARCHAR(255) NULL,
  `track_count` INT NOT NULL,
  `last_synced_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `user_id` BIGINT NOT NULL,
  `deleted_at` DATETIME NULL,
  CONSTRAINT `pk_spotify_playlists_id` PRIMARY KEY (`playlist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Playlist_Recent_Selections` (
  `user_id` BIGINT NOT NULL,
  `playlist_id` BIGINT NOT NULL,
  `selected_at` DATETIME NOT NULL,
  CONSTRAINT `pk_playlist_recent_selections_id` PRIMARY KEY (`user_id`, `playlist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Playlist_tracks` (
  `playlist_track_id` BIGINT NOT NULL AUTO_INCREMENT,
  `track_position` INT NOT NULL,
  `added_at` DATETIME NULL,
  `playlist_id` BIGINT NOT NULL,
  `track_id` BIGINT NOT NULL,
  CONSTRAINT `pk_playlist_tracks_id` PRIMARY KEY (`playlist_track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Tracks` (
  `track_id` BIGINT NOT NULL AUTO_INCREMENT,
  `spotify_track_id` VARCHAR(255) NOT NULL,
  `track_name` VARCHAR(255) NOT NULL,
  `album_name` VARCHAR(255) NULL,
  `album_image_url` VARCHAR(2048) NULL,
  `spotify_track_url` VARCHAR(2048) NULL,
  `preview_url` VARCHAR(2048) NULL,
  `duration_ms` INT NULL,
  `is_playable` BOOLEAN NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  CONSTRAINT `pk_tracks_id` PRIMARY KEY (`track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Track_Artists` (
  `track_id` BIGINT NOT NULL,
  `artist_id` BIGINT NOT NULL,
  `artist_order` SMALLINT NOT NULL,
  CONSTRAINT `pk_track_artists_id` PRIMARY KEY (`track_id`, `artist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Playlist_Analyses` (
  `analysis_id` BIGINT NOT NULL AUTO_INCREMENT,
  `spotify_playlist_id_snapshot` VARCHAR(255) NOT NULL,
  `playlist_name_snapshot` VARCHAR(255) NOT NULL,
  `playlist_image_snapshot` VARCHAR(255) NULL,
  `total_track_count` INT NOT NULL,
  `selected_artist_count` INT NOT NULL,
  `analysis_status` VARCHAR(50) NOT NULL,
  `warning_message` VARCHAR(500) NULL,
  `analyzed_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `user_id` BIGINT NOT NULL,
  `playlist_id` BIGINT NOT NULL,
  CONSTRAINT `pk_playlist_analyses_id` PRIMARY KEY (`analysis_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Analysis_Artists` (
  `artist_id` BIGINT NOT NULL,
  `analysis_id` BIGINT NOT NULL,
  `occurrence_count` INT NOT NULL,
  `popularity_snapshot` SMALLINT NULL,
  `is_major` BOOLEAN NOT NULL,
  `is_excluded` BOOLEAN NOT NULL,
  `display_rank` INT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `origin` VARCHAR(50) NOT NULL DEFAULT 'SPOTIFY_PLAYLIST',
  CONSTRAINT `pk_analysis_artists_id` PRIMARY KEY (`artist_id`, `analysis_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Venues` (
  `venue_id` BIGINT NOT NULL AUTO_INCREMENT,
  `kopis_venue_id` VARCHAR(255) NULL,
  `venue_name` VARCHAR(255) NOT NULL,
  `city` VARCHAR(255) NOT NULL,
  `district` VARCHAR(255) NULL,
  `address` VARCHAR(255) NULL,
  `latitude` DECIMAL(10,7) NULL,
  `longitude` DECIMAL(10,7) NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  CONSTRAINT `pk_venues_id` PRIMARY KEY (`venue_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performances` (
  `performance_id` BIGINT NOT NULL AUTO_INCREMENT,
  `kopis_performance_id` VARCHAR(255) NULL,
  `performance_name` VARCHAR(255) NOT NULL,
  `start_date` DATE NOT NULL,
  `end_date` DATE NOT NULL,
  `poster_url` VARCHAR(2048) NULL,
  `booking_url` VARCHAR(2048) NULL,
  `performance_status` VARCHAR(50) NOT NULL,
  `price_type` VARCHAR(50) NOT NULL,
  `ticket_price_text` VARCHAR(255) NULL,
  `favorite_count` INT NOT NULL,
  `is_deleted` BOOLEAN NOT NULL,
  `last_synced_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `venue_id` BIGINT NOT NULL,
  CONSTRAINT `pk_performances_id` PRIMARY KEY (`performance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Type_Map` (
  `performance_id` BIGINT NOT NULL,
  `performance_type_id` BIGINT NOT NULL,
  CONSTRAINT `pk_performance_type_map_id` PRIMARY KEY (`performance_id`, `performance_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Types` (
  `performance_type_id` BIGINT NOT NULL AUTO_INCREMENT,
  `type_code` VARCHAR(50) NOT NULL,
  `type_name` VARCHAR(255) NOT NULL,
  CONSTRAINT `pk_performance_types_id` PRIMARY KEY (`performance_type_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Artists` (
  `artist_id` BIGINT NOT NULL,
  `performance_id` BIGINT NOT NULL,
  `lineup_order` BIGINT NULL,
  `is_headliner` BOOLEAN NOT NULL,
  CONSTRAINT `pk_performance_artists_id` PRIMARY KEY (`artist_id`, `performance_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Genres` (
  `performance_id` BIGINT NOT NULL,
  `genre_id` BIGINT NOT NULL,
  `source_type` VARCHAR(50) NOT NULL,
  CONSTRAINT `pk_performance_genres_id` PRIMARY KEY (`performance_id`, `genre_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Matches` (
  `match_id` BIGINT NOT NULL AUTO_INCREMENT,
  `match_priority` TINYINT NOT NULL,
  `matched_artist_count` INT NOT NULL,
  `lineup_artist_count` INT NOT NULL,
  `match_ratio` TINYINT NULL,
  `recommendation_reason` VARCHAR(500) NOT NULL,
  `calculated_at` DATETIME NOT NULL,
  `performance_id` BIGINT NOT NULL,
  `analysis_id` BIGINT NOT NULL,
  `genre_id` BIGINT NULL,
  CONSTRAINT `pk_performance_matches_id` PRIMARY KEY (`match_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Match_Artists` (
  `match_id` BIGINT NOT NULL,
  `artist_id` BIGINT NOT NULL,
  `occurrence_count` INT NOT NULL,
  CONSTRAINT `pk_performance_match_artists_id` PRIMARY KEY (`match_id`, `artist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Favorite_Performances` (
  `favorite_id` BIGINT NOT NULL AUTO_INCREMENT,
  `saved_at` DATETIME NOT NULL,
  `deleted_at` DATETIME NULL,
  `user_id` BIGINT NOT NULL,
  `performance_id` BIGINT NOT NULL,
  CONSTRAINT `pk_favorite_performances_id` PRIMARY KEY (`favorite_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Views` (
  `view_id` BIGINT NOT NULL AUTO_INCREMENT,
  `viewed_at` DATETIME NOT NULL,
  `user_id` BIGINT NOT NULL,
  `analysis_id` BIGINT NOT NULL,
  `performance_id` BIGINT NOT NULL,
  CONSTRAINT `pk_performance_views_id` PRIMARY KEY (`view_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Ticket_Schedules` (
  `ticket_schedule_id` BIGINT NOT NULL AUTO_INCREMENT,
  `schedule_name` VARCHAR(255) NOT NULL,
  `sale_type` VARCHAR(50) NOT NULL,
  `opens_at` DATETIME NOT NULL,
  `closes_at` DATETIME NULL,
  `booking_url` VARCHAR(2048) NULL,
  `sale_status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `performance_id` BIGINT NOT NULL,
  CONSTRAINT `pk_ticket_schedules_id` PRIMARY KEY (`ticket_schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Calendar_Entries` (
  `calendar_entry_id` BIGINT NOT NULL AUTO_INCREMENT,
  `calendar_at` DATETIME NOT NULL,
  `external_event_id` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL,
  `user_id` BIGINT NOT NULL,
  `ticket_schedule_id` BIGINT NOT NULL,
  CONSTRAINT `pk_calendar_entries_id` PRIMARY KEY (`calendar_entry_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Prestudy_Playlists` (
  `prestudy_playlist_id` BIGINT NOT NULL AUTO_INCREMENT,
  `spotify_playlist_id` VARCHAR(255) NULL,
  `playlist_title` VARCHAR(255) NOT NULL,
  `is_public` BOOLEAN NOT NULL,
  `track_count` INT NOT NULL,
  `spotify_deleted` BOOLEAN NOT NULL,
  `creation_status` VARCHAR(50) NOT NULL,
  `created_at` DATETIME NOT NULL,
  `updated_at` DATETIME NOT NULL,
  `user_id` BIGINT NOT NULL,
  `performance_id` BIGINT NOT NULL,
  `analysis_id` BIGINT NULL,
  CONSTRAINT `pk_prestudy_playlists_id` PRIMARY KEY (`prestudy_playlist_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Prestudy_Playlist_Tracks` (
  `prestudy_playlist_id` BIGINT NOT NULL,
  `track_id` BIGINT NOT NULL,
  `track_order` INT NOT NULL,
  `source_type` VARCHAR(50) NOT NULL,
  `is_new_artist_track` BOOLEAN NOT NULL,
  CONSTRAINT `pk_prestudy_playlist_tracks_id` PRIMARY KEY (`prestudy_playlist_id`, `track_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE `Spotify_accounts`
  ADD CONSTRAINT `fk_spotify_accounts_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Auth_Refresh_Tokens`
  ADD CONSTRAINT `fk_auth_refresh_tokens_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Spotify_account_scopes`
  ADD CONSTRAINT `fk_spotify_account_scopes_spotify_account_id`
  FOREIGN KEY (`spotify_account_id`) REFERENCES `Spotify_accounts` (`spotify_account_id`);

ALTER TABLE `Artists_Genres`
  ADD CONSTRAINT `fk_artists_genres_artist_id`
  FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`);

ALTER TABLE `Artists_Genres`
  ADD CONSTRAINT `fk_artists_genres_genre_id`
  FOREIGN KEY (`genre_id`) REFERENCES `Genres` (`genre_id`);

ALTER TABLE `Spotify_Playlists`
  ADD CONSTRAINT `fk_spotify_playlists_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Playlist_Recent_Selections`
  ADD CONSTRAINT `fk_playlist_recent_selections_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Playlist_Recent_Selections`
  ADD CONSTRAINT `fk_playlist_recent_selections_playlist_id`
  FOREIGN KEY (`playlist_id`) REFERENCES `Spotify_Playlists` (`playlist_id`);

ALTER TABLE `Playlist_tracks`
  ADD CONSTRAINT `fk_playlist_tracks_playlist_id`
  FOREIGN KEY (`playlist_id`) REFERENCES `Spotify_Playlists` (`playlist_id`);

ALTER TABLE `Playlist_tracks`
  ADD CONSTRAINT `fk_playlist_tracks_track_id`
  FOREIGN KEY (`track_id`) REFERENCES `Tracks` (`track_id`);

ALTER TABLE `Track_Artists`
  ADD CONSTRAINT `fk_track_artists_track_id`
  FOREIGN KEY (`track_id`) REFERENCES `Tracks` (`track_id`);

ALTER TABLE `Track_Artists`
  ADD CONSTRAINT `fk_track_artists_artist_id`
  FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`);

ALTER TABLE `Playlist_Analyses`
  ADD CONSTRAINT `fk_playlist_analyses_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Playlist_Analyses`
  ADD CONSTRAINT `fk_playlist_analyses_playlist_id`
  FOREIGN KEY (`playlist_id`) REFERENCES `Spotify_Playlists` (`playlist_id`);

ALTER TABLE `Analysis_Artists`
  ADD CONSTRAINT `fk_analysis_artists_artist_id`
  FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`);

ALTER TABLE `Analysis_Artists`
  ADD CONSTRAINT `fk_analysis_artists_analysis_id`
  FOREIGN KEY (`analysis_id`) REFERENCES `Playlist_Analyses` (`analysis_id`);

ALTER TABLE `Performances`
  ADD CONSTRAINT `fk_performances_venue_id`
  FOREIGN KEY (`venue_id`) REFERENCES `Venues` (`venue_id`);

ALTER TABLE `Performance_Type_Map`
  ADD CONSTRAINT `fk_performance_type_map_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Performance_Type_Map`
  ADD CONSTRAINT `fk_performance_type_map_performance_type_id`
  FOREIGN KEY (`performance_type_id`) REFERENCES `Performance_Types` (`performance_type_id`);

ALTER TABLE `Performance_Artists`
  ADD CONSTRAINT `fk_performance_artists_artist_id`
  FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`);

ALTER TABLE `Performance_Artists`
  ADD CONSTRAINT `fk_performance_artists_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Performance_Genres`
  ADD CONSTRAINT `fk_performance_genres_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Performance_Genres`
  ADD CONSTRAINT `fk_performance_genres_genre_id`
  FOREIGN KEY (`genre_id`) REFERENCES `Genres` (`genre_id`);

ALTER TABLE `Performance_Matches`
  ADD CONSTRAINT `fk_performance_matches_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Performance_Matches`
  ADD CONSTRAINT `fk_performance_matches_analysis_id`
  FOREIGN KEY (`analysis_id`) REFERENCES `Playlist_Analyses` (`analysis_id`);

ALTER TABLE `Performance_Matches`
  ADD CONSTRAINT `fk_performance_matches_genre_id`
  FOREIGN KEY (`genre_id`) REFERENCES `Genres` (`genre_id`);

ALTER TABLE `Performance_Match_Artists`
  ADD CONSTRAINT `fk_performance_match_artists_match_id`
  FOREIGN KEY (`match_id`) REFERENCES `Performance_Matches` (`match_id`);

ALTER TABLE `Performance_Match_Artists`
  ADD CONSTRAINT `fk_performance_match_artists_artist_id`
  FOREIGN KEY (`artist_id`) REFERENCES `Artists` (`artist_id`);

ALTER TABLE `Favorite_Performances`
  ADD CONSTRAINT `fk_favorite_performances_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Favorite_Performances`
  ADD CONSTRAINT `fk_favorite_performances_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Performance_Views`
  ADD CONSTRAINT `fk_performance_views_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Performance_Views`
  ADD CONSTRAINT `fk_performance_views_analysis_id`
  FOREIGN KEY (`analysis_id`) REFERENCES `Playlist_Analyses` (`analysis_id`);

ALTER TABLE `Performance_Views`
  ADD CONSTRAINT `fk_performance_views_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Ticket_Schedules`
  ADD CONSTRAINT `fk_ticket_schedules_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Calendar_Entries`
  ADD CONSTRAINT `fk_calendar_entries_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Calendar_Entries`
  ADD CONSTRAINT `fk_calendar_entries_ticket_schedule_id`
  FOREIGN KEY (`ticket_schedule_id`) REFERENCES `Ticket_Schedules` (`ticket_schedule_id`);

ALTER TABLE `Prestudy_Playlists`
  ADD CONSTRAINT `fk_prestudy_playlists_user_id`
  FOREIGN KEY (`user_id`) REFERENCES `Users` (`user_id`);

ALTER TABLE `Prestudy_Playlists`
  ADD CONSTRAINT `fk_prestudy_playlists_performance_id`
  FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`);

ALTER TABLE `Prestudy_Playlists`
  ADD CONSTRAINT `fk_prestudy_playlists_analysis_id`
  FOREIGN KEY (`analysis_id`) REFERENCES `Playlist_Analyses` (`analysis_id`);

ALTER TABLE `Prestudy_Playlist_Tracks`
  ADD CONSTRAINT `fk_prestudy_playlist_tracks_prestudy_playlist_id`
  FOREIGN KEY (`prestudy_playlist_id`) REFERENCES `Prestudy_Playlists` (`prestudy_playlist_id`);

ALTER TABLE `Prestudy_Playlist_Tracks`
  ADD CONSTRAINT `fk_prestudy_playlist_tracks_track_id`
  FOREIGN KEY (`track_id`) REFERENCES `Tracks` (`track_id`);

ALTER TABLE `Spotify_accounts`
  ADD CONSTRAINT `uq_spotify_accounts_user_id` UNIQUE (`user_id`);

ALTER TABLE `Auth_Refresh_Tokens`
  ADD CONSTRAINT `uq_auth_refresh_tokens_token_hash` UNIQUE (`token_hash`);

ALTER TABLE `Spotify_accounts`
  ADD CONSTRAINT `uq_spotify_accounts_spotify_user_id` UNIQUE (`spotify_user_id`);

ALTER TABLE `Spotify_Playlists`
  ADD CONSTRAINT `uq_spotify_playlists_user_id_spotify_playlist_id` UNIQUE (`user_id`, `spotify_playlist_id`);

ALTER TABLE `Playlist_tracks`
  ADD CONSTRAINT `uq_playlist_tracks_playlist_id_track_id` UNIQUE (`playlist_id`, `track_id`);

ALTER TABLE `Playlist_tracks`
  ADD CONSTRAINT `uq_playlist_tracks_playlist_id_track_position` UNIQUE (`playlist_id`, `track_position`);

ALTER TABLE `Playlist_Analyses`
  ADD CONSTRAINT `uq_playlist_analyses_user_id_playlist_id` UNIQUE (`user_id`, `playlist_id`);

ALTER TABLE `Performance_Matches`
  ADD CONSTRAINT `uq_performance_matches_analysis_id_performance_id` UNIQUE (`analysis_id`, `performance_id`);

ALTER TABLE `Favorite_Performances`
  ADD CONSTRAINT `uq_favorite_performances_user_id_performance_id` UNIQUE (`user_id`, `performance_id`);

ALTER TABLE `Performance_Views`
  ADD CONSTRAINT `uq_performance_views_user_id_analysis_id_performance_id` UNIQUE (`user_id`, `analysis_id`, `performance_id`);

ALTER TABLE `Calendar_Entries`
  ADD CONSTRAINT `uq_calendar_entries_user_id_ticket_schedule_id` UNIQUE (`user_id`, `ticket_schedule_id`);

ALTER TABLE `Tracks`
  ADD CONSTRAINT `uq_tracks_spotify_track_id` UNIQUE (`spotify_track_id`);

ALTER TABLE `Artists`
  ADD CONSTRAINT `uq_artists_spotify_artist_id` UNIQUE (`spotify_artist_id`);

ALTER TABLE `Venues`
  ADD CONSTRAINT `uq_venues_kopis_venue_id` UNIQUE (`kopis_venue_id`);

ALTER TABLE `Performances`
  ADD CONSTRAINT `uq_performances_kopis_performance_id` UNIQUE (`kopis_performance_id`);

SET FOREIGN_KEY_CHECKS = 1;
