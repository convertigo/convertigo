SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0;
SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0;
SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='TRADITIONAL';

DROP SCHEMA IF EXISTS `CemsBilling` ;
CREATE SCHEMA IF NOT EXISTS `CemsBilling`;
USE `CemsBilling`;

-- -----------------------------------------------------
-- Table `CemsBilling`.`TicketTable`
-- -----------------------------------------------------
DROP TABLE IF EXISTS `CemsBilling`.`Ticket` ;

CREATE  TABLE IF NOT EXISTS `CemsBilling`.`Ticket` (
  `Id` BIGINT NOT NULL AUTO_INCREMENT ,
  `CreationDate` BIGINT NOT NULL ,
  `ClientIp` VARCHAR(45) NOT NULL ,
  `CustomerName` VARCHAR(45) NOT NULL ,
  `UserName` VARCHAR(45) NOT NULL ,
  `ProjectName` VARCHAR(45) NOT NULL ,
  `ConnectorName` VARCHAR(45) NOT NULL ,
  `ConnectorType` VARCHAR(45) NOT NULL ,
  `RequestableName` VARCHAR(45) NOT NULL ,
  `RequestableType` VARCHAR(45) NOT NULL ,
  `ResponseTime` BIGINT NOT NULL DEFAULT 0 ,
  `Score` BIGINT NOT NULL DEFAULT 0 ,
  PRIMARY KEY (`Id`) )
ENGINE = InnoDB
DEFAULT CHARACTER SET = latin1;



SET SQL_MODE=@OLD_SQL_MODE;
SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS;
SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS;
