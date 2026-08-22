ALTER TABLE `Performances`
  ADD COLUMN `running_time` VARCHAR(255) NULL AFTER `ticket_price_text`,
  ADD COLUMN `age_restriction` VARCHAR(255) NULL AFTER `running_time`;
