ALTER TABLE `Users`
  ADD COLUMN `nickname` VARCHAR(20) NULL AFTER `status`,
  ADD COLUMN `birth_date` DATE NULL AFTER `nickname`;
