ALTER TABLE `Playlist_Analyses`
  ADD INDEX `idx_playlist_analyses_user_id` (`user_id`),
  DROP INDEX `uq_playlist_analyses_user_id_playlist_id`;
