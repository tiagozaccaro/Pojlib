package pojlib.api;

import android.app.Activity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.json.JSONException;

import pojlib.account.MinecraftAccount;
import pojlib.install.*;
import pojlib.instance.MinecraftInstance;
import pojlib.util.APIHandler;
import pojlib.util.Constants;
import pojlib.util.StaticProperty;
import pojlib.util.StaticPropertyObserver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

/**
 * This class is the only class used by the launcher to communicate and talk to pojlib. This keeps pojlib and launcher separate.
 * If we ever make breaking change to either project, we can make a new api class to accommodate for those changes without
 * having to make changes to either project deeply.
 */
public class API_V1 {

    private static boolean hasQueried = false;
    private static JsonObject initialResponse;

    private static String msaMessage = "";
    private static boolean finishedDownloading = false;
    private static boolean ignoreInstanceName = false;
    private static boolean customRAMValue = false;
    private static double downloadStatus = 0.0;
    private static String currentDownload = "";
    private static String profileImage = "";
    private static String profileName = "";
    private static String memoryValue = "3072";
    private static boolean developerMods = false;
    private static boolean advancedDebugger = false;

    public static String getMsaMessage() {
        return msaMessage;
    }

    public static void setMsaMessage(String msaMessage) {
        if (!API_V1.msaMessage.equals(msaMessage)) {
            API_V1.msaMessage = msaMessage;
            notifyObservers(StaticProperty.MSA_MESSAGE);
        }
    }

    public static boolean isFinishedDownloading() {
        return finishedDownloading;
    }

    public static void setFinishedDownloading(boolean finishedDownloading) {
        if (API_V1.finishedDownloading != finishedDownloading) {
            API_V1.finishedDownloading = finishedDownloading;
            notifyObservers(StaticProperty.FINISHED_DOWNLOADING);
        }
    }

    public static boolean isIgnoreInstanceName() {
        return ignoreInstanceName;
    }

    public static void setIgnoreInstanceName(boolean ignoreInstanceName) {
        if (API_V1.ignoreInstanceName != ignoreInstanceName) {
            API_V1.ignoreInstanceName = ignoreInstanceName;
            notifyObservers(StaticProperty.IGNORE_INSTANCE_NAME);
        }
    }

    public static boolean isCustomRAMValue() {
        return customRAMValue;
    }

    public static void setCustomRAMValue(boolean customRAMValue) {
        if (API_V1.customRAMValue != customRAMValue) {
            API_V1.customRAMValue = customRAMValue;
            notifyObservers(StaticProperty.CUSTOM_RAM_VALUE);
        }
    }

    public static double getDownloadStatus() {
        return downloadStatus;
    }

    public static void setDownloadStatus(double downloadStatus) {
        if (API_V1.downloadStatus != downloadStatus) {
            API_V1.downloadStatus = downloadStatus;
            notifyObservers(StaticProperty.DOWNLOAD_STATUS);
        }
    }

    public static String getCurrentDownload() {
        return currentDownload;
    }

    public static void setCurrentDownload(String currentDownload) {
        if (!API_V1.getCurrentDownload().equals(currentDownload)) {
            API_V1.currentDownload = currentDownload;
            notifyObservers(StaticProperty.CURRENT_DOWNLOAD);
        }
    }

    public static String getProfileImage() {
        return profileImage;
    }

    public static void setProfileImage(String profileImage) {
        if (!API_V1.profileImage.equals(profileImage)) {
            API_V1.profileImage = profileImage;
            notifyObservers(StaticProperty.PROFILE_IMAGE);
        }
    }

    public static String getProfileName() {
        return profileName;
    }

    public static void setProfileName(String profileName) {
        if (!API_V1.profileName.equals(profileName)) {
            API_V1.profileName = profileName;
            notifyObservers(StaticProperty.PROFILE_NAME);
        }
    }

    public static String getMemoryValue() {
        return memoryValue;
    }

    public static void setMemoryValue(String memoryValue) {
        if (!API_V1.memoryValue.equals(memoryValue)) {
            API_V1.memoryValue = memoryValue;
            notifyObservers(StaticProperty.MEMORY_VALUE);
        }
    }

    public static boolean isDeveloperMods() {
        return developerMods;
    }

    public static void setDeveloperMods(boolean developerMods) {
        if (API_V1.developerMods != developerMods) {
            API_V1.developerMods = developerMods;
            notifyObservers(StaticProperty.DEVELOPER_MODS);
        }
    }

    public static boolean isAdvancedDebugger() {
        return advancedDebugger;
    }

    public static void setAdvancedDebugger(boolean advancedDebugger) {
        if (API_V1.advancedDebugger != advancedDebugger) {
            API_V1.advancedDebugger = advancedDebugger;
            notifyObservers(StaticProperty.ADVANCED_DEBUGGER);
        }
    }

    private static List<StaticPropertyObserver> observers = new ArrayList<>();

    public static void addObserver(StaticPropertyObserver observer) {
        observers.add(observer);
    }

    public static void removeObserver(StaticPropertyObserver observer) {
        observers.remove(observer);
    }

    private static void notifyObservers(StaticProperty property) {
        for (StaticPropertyObserver observer : observers) {
            observer.onStaticPropertyChanged(property);
        }
    }

    /**
     * @return A list of every minecraft version
     */
    public static MinecraftMeta.MinecraftVersion[] getMinecraftVersions() {
        return MinecraftMeta.getVersions();
    }

    public static void addCustomMod(MinecraftInstance instance, String name, String version, String url) {
        instance.addCustomMod(name, version, url);
    }

    public static boolean hasMod(MinecraftInstance instance, String name) {
        return instance.hasCustomMod(name);
    }

    /**
     * @return if the operation succeeds
     */
    public static boolean removeMod(MinecraftInstance instance, String name) {
        return instance.removeMod(name);
    }
    public static MinecraftMeta.MinecraftVersion[] getQCSupportedVersions() {
        return APIHandler.getQCSupportedVersions();
    }

    public static String getQCSupportedVersionName(MinecraftMeta.MinecraftVersion version) {
        return APIHandler.getQCSupportedVersionName(version);
    }

    /**
     * A collection of loader types
     */
    public enum ModLoader {
        Vanilla(0),
        Fabric(1),
        Quilt(2);

        public final int index;

        ModLoader(int i) {
            this.index = i;
        }
    }

    /**
     * Loads an instance from the filesystem.
     *
     * @param instanceName      The instance being loaded
     * @param gameDir           .minecraft directory.
     * @return                  A minecraft instance object
     */
    public static MinecraftInstance load(String instanceName, String gameDir) {
        return MinecraftInstance.load(instanceName, gameDir);
    }

    /**
     * Creates a new game instance with a selected mod loader. The latest version of the mod loader will be installed
     *
     * @param activity          The active android activity
     * @param instanceName      The name of the instance being created - can be anything, used for identification
     * @param home              The base directory where minecraft should be setup
     * @param minecraftVersion  The version of minecraft to install
     * @return                  A minecraft instance object
     * @throws                  IOException Throws if download of library or asset fails
     */
    public static MinecraftInstance createNewInstance(Activity activity, String instanceName, String home, MinecraftMeta.MinecraftVersion minecraftVersion) throws IOException {

        if(ignoreInstanceName) {
            return MinecraftInstance.create(activity, instanceName, home, minecraftVersion);
        } else if (instanceName.contains("/") || instanceName.contains("!")) {
            throw new IOException("You cannot use special characters (!, /, ., etc) when creating instances.");
        } else {
            return MinecraftInstance.create(activity, instanceName, home, minecraftVersion);
        }
    }

    /**
     * Logs the user in and keeps them logged in unless they log out
     *
     * @param home      The base directory where minecraft should be setup
     * @param authCode  The token received from the microsoft login window
     * @return          A minecraft account object
     */
    public static MinecraftAccount login(String home, String authCode, JsonObject response) throws IOException, JSONException {
        return MinecraftAccount.login(home, response);
    }

    public static void launchInstance(Activity activity, MinecraftAccount account, MinecraftInstance instance) {
        instance.launchInstance(activity, account);
    }

    /**
     * Fetches the account data from disk if the user has logged in before.
     *
     * @param home  The base directory where minecraft should be setup
     * @return      A minecraft account object, null if no account found
     */
    public static MinecraftAccount fetchSavedLogin(String home, String client_id) {
        return MinecraftAccount.load(home, client_id);
    }

    /**
     * Logs the user out
     *
     * @param home  The base directory where minecraft should be setup
     * @return      True if logout was successful
     */
    public static boolean logout(String home) {
        return MinecraftAccount.logout(home);
    }

    public static MinecraftAccount login(String client_id, Activity activity)
    {
        MinecraftAccount acc = MinecraftAccount.load(activity.getFilesDir() + "/accounts", client_id);
        if(acc != null) {
            profileImage = MinecraftAccount.getSkinFaceUrl(acc);
            profileName = MinecraftAccount.username;
            return acc;
        }
        try {
            // Stage 1
            if(!hasQueried) {
                URLConnection connection = new URL("https://login.microsoftonline.com/consumers/oauth2/v2.0/devicecode").openConnection();
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String query = String.format("client_id=%s&scope=%s",
                        URLEncoder.encode(client_id, "UTF-8"),
                        URLEncoder.encode("XboxLive.signin Xboxlive.offline_access", "UTF-8"));
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(query.getBytes(StandardCharsets.UTF_8));
                }

                InputStream response = connection.getInputStream();

                initialResponse = (JsonObject) JsonParser.parseReader(new InputStreamReader(response, StandardCharsets.UTF_8));

                if(initialResponse.get("message") != null) {
                    msaMessage = initialResponse.get("message").getAsString();
                    hasQueried = true;
                    return null;
                }

                File errorFile = new File(Constants.USER_HOME + "/errors.txt");
                BufferedWriter writer = new BufferedWriter(new FileWriter(errorFile));
                writer.write(initialResponse.toString());
                writer.flush();

                throw new RuntimeException();
            }

            if(hasQueried) {
                URLConnection connection2 = new URL("https://login.microsoftonline.com/consumers/oauth2/v2.0/token").openConnection();
                connection2.setDoOutput(true);
                connection2.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

                String query2 = String.format("client_id=%s&grant_type=%s&device_code=%s",
                        URLEncoder.encode(client_id, "UTF-8"),
                        URLEncoder.encode("urn:ietf:params:oauth:grant-type:device_code", "UTF-8"),
                        URLEncoder.encode(initialResponse.get("device_code").getAsString(), "UTF-8"));
                try (OutputStream output = connection2.getOutputStream()) {
                    output.write(query2.getBytes(StandardCharsets.UTF_8));
                }

                InputStream response2 = connection2.getInputStream();

                JsonObject jsonObject2 = (JsonObject) JsonParser.parseReader(new InputStreamReader(response2, StandardCharsets.UTF_8));

                if(jsonObject2.get("access_token") != null) {
                    // Finally, log in
                    acc = MinecraftAccount.login(activity.getFilesDir() + "/accounts", jsonObject2);
                    profileImage = MinecraftAccount.getSkinFaceUrl(acc);
                    profileName = MinecraftAccount.username;
                    return acc;
                }

                File errorFile = new File(Constants.USER_HOME + "/errors.txt");
                BufferedWriter writer = new BufferedWriter(new FileWriter(errorFile));
                writer.write(jsonObject2.toString());
                writer.flush();

                throw new RuntimeException();
            }
            hasQueried = true;
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

}
