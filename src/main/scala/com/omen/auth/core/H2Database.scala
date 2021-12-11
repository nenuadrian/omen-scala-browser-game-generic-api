package com.omen.auth.core

import org.apache.commons.dbcp2.BasicDataSource

import scala.util.Random

trait H2Database {

  protected def generateDataSource: BasicDataSource = {
    val clientConnPool = new BasicDataSource()
    clientConnPool.setDriverClassName("org.h2.Driver")
    clientConnPool.setUrl("jdbc:h2:~/test3" + new Random().nextInt())
    clientConnPool.setInitialSize(2)
    clientConnPool.setUsername("sa")
    clientConnPool.setPassword("")
    clientConnPool
  }

  def refresh(clientConnPool: BasicDataSource): Unit = {
    val conn = clientConnPool.getConnection
    conn.prepareStatement("DROP TABLE IF EXISTS session; CREATE TABLE session ( session_id VARCHAR(256) NOT NULL, account_type VARCHAR(256) NOT NULL, account_id VARCHAR(256) NOT NULL, status INT(1) default 1, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP );").execute()
    conn.prepareStatement("DROP TABLE IF EXISTS email_confirmation; CREATE TABLE email_confirmation ( id bigint auto_increment, token VARCHAR(256) NOT NULL, account_id VARCHAR(256), disabled_at TIMESTAMP default null, account_type varchar(45) not null, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);").execute()
    conn.prepareStatement("DROP TABLE IF EXISTS forgot_password; CREATE TABLE forgot_password ( id bigint auto_increment, token VARCHAR(256) NOT NULL, account_id VARCHAR(256), disabled_at TIMESTAMP default null, account_type varchar(45) not null, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);").execute()
    conn.prepareStatement("DROP TABLE IF EXISTS user; CREATE TABLE user ( user_id bigint auto_increment, password VARCHAR(512) NOT NULL, email VARCHAR(256), email_confirmed_at TIMESTAMP default null, group_id INT(3) default 1, last_password_change TIMESTAMP default null, deleted_at TIMESTAMP default null, status INT(1) default 1, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);").execute()
    conn.prepareStatement("DROP TABLE IF EXISTS log; CREATE TABLE log ( log_id bigint auto_increment, reference_id bigint default null, reference_id_secondary bigint default null, identifier VARCHAR(512) NOT NULL, data TEXT default null, reference_timestamp TIMESTAMP default null, entity_type varchar(255), timestamp bigint default null, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP);").execute()
    conn.close()
  }
}
