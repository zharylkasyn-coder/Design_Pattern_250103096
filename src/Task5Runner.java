
package task5;

class UserSession {
    private final String username, role;

    UserSession(String username, String role) {
        this.username = username;
        this.role = role;
    }

    String getUsername() { return username; }
    String getRole() { return role; }
}

interface StorageDriver {
    byte[] loadRawBlob(String path);
    String getStorageType();
}

class S3StorageDriver implements StorageDriver {
    public byte[] loadRawBlob(String path) {
        System.out.println("[AWS S3 CLIENT] Performing HTTPS GET on bucket uri: s3://" + path);
        return new byte[]{0x1F, (byte) 0x8B, 0x08, 0x00};
    }

    public String getStorageType() {
        return "AMAZON_S3_CLOUD";
    }
}

class NfsStorageDriver implements StorageDriver {
    public byte[] loadRawBlob(String path) {
        System.out.println("[NFS POSIX CLIENT] Performing direct IO read on /mnt/nfs/" + path);
        return new byte[]{0x4F, 0x67, 0x67, 0x53};
    }

    public String getStorageType() {
        return "LOCAL_NFS_CLUSTER";
    }
}

class StorageDriverProxy implements StorageDriver {
    private final StorageDriver driver;
    private final UserSession session;

    StorageDriverProxy(StorageDriver driver, UserSession session) {
        this.driver = driver;
        this.session = session;
    }

    public byte[] loadRawBlob(String path) {
        if (session.getRole().equalsIgnoreCase("GUEST")) {
            System.out.printf(
                    "[PROXY BLOCKED] Access Denied for User '%s' [Role: %s] on resource: %s%n",
                    session.getUsername(), session.getRole(), path);
            return new byte[0];
        }

        System.out.printf(
                "[PROXY AUDIT] Authorized user '%s' [Role: %s] accessing backend: %s%n",
                session.getUsername(), session.getRole(), driver.getStorageType());

        return driver.loadRawBlob(path);
    }

    public String getStorageType() {
        return "PROXY_GUARDED(" + driver.getStorageType() + ")";
    }
}

abstract class DigitalAsset {
    protected final StorageDriver driver;
    protected final String path;

    DigitalAsset(StorageDriver driver, String path) {
        this.driver = driver;
        this.path = path;
    }

    public abstract void render();
}

class VideoAsset extends DigitalAsset {
    VideoAsset(StorageDriver driver, String path) {
        super(driver, path);
    }

    public void render() {
        System.out.println("Initializing Video Player for: " + path);
        byte[] data = driver.loadRawBlob(path);

        if (data.length > 0)
            System.out.printf("SUCCESS: Video playback buffered %d bytes via %s%n",
                    data.length, driver.getStorageType());
        else
            System.out.println("FAILURE: Video buffer empty. Playback aborted.");
    }
}

class AudioAsset extends DigitalAsset {
    AudioAsset(StorageDriver driver, String path) {
        super(driver, path);
    }

    public void render() {
        System.out.println("Initializing Audio Stream for: " + path);
        byte[] data = driver.loadRawBlob(path);

        if (data.length > 0)
            System.out.printf("SUCCESS: Audio bitstream synchronized %d bytes via %s%n",
                    data.length, driver.getStorageType());
        else
            System.out.println("FAILURE: Audio stream inaccessible.");
    }
}

public class Task5Runner {
    public static void main(String[] args) {
        StorageDriver s3 = new S3StorageDriver();
        StorageDriver nfs = new NfsStorageDriver();

        UserSession admin = new UserSession("sufyan", "ADMIN");
        UserSession guest = new UserSession("anonymous", "GUEST");

        System.out.println(">>> 1. Authorized Video Playback via S3 (Admin Role) <<<");
        DigitalAsset video = new VideoAsset(
                new StorageDriverProxy(s3, admin), "trailers/hyperion_4k.mp4");
        video.render();

        System.out.println("\n>>> 2. Unauthorized Audio Streaming via NFS (Guest Role - Blocked) <<<");
        DigitalAsset audio = new AudioAsset(
                new StorageDriverProxy(nfs, guest), "master_audio/soundtrack.flac");
        audio.render();
    }
}
