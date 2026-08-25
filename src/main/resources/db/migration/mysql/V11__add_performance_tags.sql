CREATE TABLE `Performance_Tags` (
  `performance_tag_id` BIGINT NOT NULL AUTO_INCREMENT,
  `tag_code` VARCHAR(50) NOT NULL,
  `tag_name` VARCHAR(255) NOT NULL,
  CONSTRAINT `pk_performance_tags_id` PRIMARY KEY (`performance_tag_id`),
  CONSTRAINT `uq_performance_tags_tag_code` UNIQUE (`tag_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `Performance_Tag_Map` (
  `performance_id` BIGINT NOT NULL,
  `performance_tag_id` BIGINT NOT NULL,
  CONSTRAINT `pk_performance_tag_map_id` PRIMARY KEY (`performance_id`, `performance_tag_id`),
  CONSTRAINT `fk_performance_tag_map_performance_id`
    FOREIGN KEY (`performance_id`) REFERENCES `Performances` (`performance_id`),
  CONSTRAINT `fk_performance_tag_map_performance_tag_id`
    FOREIGN KEY (`performance_tag_id`) REFERENCES `Performance_Tags` (`performance_tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
