
package com.zy.jdbclib.example;

// default package

import java.util.Date;

/**
 * @version 1.0
 * @since 1.0
 */
public class Admin implements java.io.Serializable {

    // Fields

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private Integer id;

    private String username;

    private String password;

    private String lastIp;

    private Date lastTime;

    // Constructors

    /** default constructor */
    public Admin() {
    }

    /** minimal constructor */
    public Admin(String username, String password, String lastIp, Date lastTime) {
        this.username = username;
        this.password = password;
        this.lastIp = lastIp;
        this.lastTime = lastTime;
    }

    // Property accessors
    public Integer getId() {
        return this.id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getLastIp() {
        return this.lastIp;
    }

    public void setLastIp(String lastIp) {
        this.lastIp = lastIp;
    }

    public Date getLastTime() {
        return this.lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(this.getClass().getName() + "[");
        sb.append("id=" + this.id + ",");
        sb.append("username=" + this.username + ",");
        sb.append("password=" + this.password + ",");
        sb.append("lastIp=" + this.lastIp + ",");
        sb.append("lastTime=" + this.lastTime + "]");
        return sb.toString();
    }

}
