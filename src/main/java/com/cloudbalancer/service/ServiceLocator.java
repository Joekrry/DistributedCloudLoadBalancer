package com.cloudbalancer.service;

import com.cloudbalancer.dao.UserDAO;
import com.cloudbalancer.database.LocalDatabase;
import com.cloudbalancer.database.RemoteDatabase;

public class ServiceLocator {
    private static final LocalDatabase localDb = new LocalDatabase();
    private static final RemoteDatabase remoteDb = new RemoteDatabase();
    private static final UserDAO userDAO = new UserDAO(localDb, remoteDb);

    public static LocalDatabase getLocalDb() { return localDb; }
    public static RemoteDatabase getRemoteDb() { return remoteDb; }
    public static UserDAO getUserDAO() { return userDAO; }
}
