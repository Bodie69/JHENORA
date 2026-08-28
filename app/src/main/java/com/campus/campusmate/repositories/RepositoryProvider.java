package com.campus.campusmate.repositories;

import android.content.Context;

/**
 * Centralized provider for all repositories in the application.
 * This simplifies dependency management and ensures consistent initialization.
 */
public class RepositoryProvider {

    private static RepositoryProvider instance;
    
    private final UserRepository userRepository;
    private final AnnouncementRepository announcementRepository;
    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final ImageStorageRepository imageStorageRepository;

    private RepositoryProvider(Context context) {
        Context appContext = context.getApplicationContext();
        
        userRepository = UserRepository.getInstance();
        userRepository.init(appContext);
        
        announcementRepository = AnnouncementRepository.getInstance();
        announcementRepository.init(appContext);
        
        eventRepository = EventRepository.getInstance();
        eventRepository.init(appContext);
        
        organizationRepository = OrganizationRepository.getInstance();
        organizationRepository.init(appContext);
        
        imageStorageRepository = ImageStorageRepository.getInstance();
        imageStorageRepository.init(appContext);
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new RepositoryProvider(context);
        }
    }

    public static RepositoryProvider getInstance() {
        if (instance == null) {
            throw new IllegalStateException("RepositoryProvider must be initialized in Application.onCreate()");
        }
        return instance;
    }

    public UserRepository getUserRepository() {
        return userRepository;
    }

    public AnnouncementRepository getAnnouncementRepository() {
        return announcementRepository;
    }

    public EventRepository getEventRepository() {
        return eventRepository;
    }

    public OrganizationRepository getOrganizationRepository() {
        return organizationRepository;
    }

    public ImageStorageRepository getImageStorageRepository() {
        return imageStorageRepository;
    }
}
