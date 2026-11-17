package history;

/**
 * Registry and factory for the active StorageService.
 */
public class StorageManager {
    private static StorageService instance;

    public static synchronized StorageService getService() {
        if (instance == null) {
            String type = System.getProperty("arena.storage");
            if ("memory".equalsIgnoreCase(type)) {
                instance = new InMemoryStorageService();
            } else {
                try {
                    instance = new CsvStorageService();
                } catch (Exception e) {
                    System.err.println("[StorageManager] Warning: Failed to initialize CSV storage, falling back to in-memory: " + e.getMessage());
                    instance = new InMemoryStorageService();
                }
            }
        }
        return instance;
    }

    public static synchronized void setService(StorageService service) {
        instance = service;
    }
}
