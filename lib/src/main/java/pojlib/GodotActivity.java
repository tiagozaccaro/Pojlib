package pojlib;

import android.app.Activity;
import android.util.ArraySet;
import android.view.View;

import androidx.annotation.NonNull;

import org.apache.commons.io.FileUtils;
import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.plugin.GodotPlugin;
import org.godotengine.godot.plugin.SignalInfo;
import org.godotengine.godot.plugin.UsedByGodot;
import org.json.JSONException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.util.Arrays;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import pojlib.account.MinecraftAccount;
import pojlib.api.API_V1;
import pojlib.install.MinecraftMeta;
import pojlib.instance.MinecraftInstance;
import pojlib.util.Constants;
import pojlib.util.FileUtil;
import pojlib.util.StaticProperty;
import pojlib.util.StaticPropertyObserver;

public class GodotActivity extends GodotPlugin implements StaticPropertyObserver {

    public GodotActivity(Godot godot) {
        super(godot);
    }

    @Override
    public View onMainCreate(Activity activity) {
        File file = new File(activity.getFilesDir() + "/runtimes/JRE-17.zip");
        if (!file.exists()) {
            try {
                FileUtils.writeByteArrayToFile(file, FileUtil.loadFromAssetToByte(activity, "JRE-21.zip"));
                byte[] bArr = new byte[1024];
                ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(file.toPath(), new OpenOption[0]));
                for (ZipEntry nextEntry = zipInputStream.getNextEntry(); nextEntry != null; nextEntry = zipInputStream.getNextEntry()) {
                    File newFile = newFile(new File(activity.getFilesDir() + "/runtimes/JRE-21"), nextEntry);
                    if (nextEntry.isDirectory()) {
                        if (!newFile.isDirectory() && !newFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + newFile);
                        }
                    } else {
                        File parentFile = newFile.getParentFile();
                        if (!parentFile.isDirectory() && !parentFile.mkdirs()) {
                            throw new IOException("Failed to create directory " + parentFile);
                        }
                        FileOutputStream fileOutputStream = new FileOutputStream(newFile);
                        while (true) {
                            int read = zipInputStream.read(bArr);
                            if (read <= 0) {
                                break;
                            }
                            fileOutputStream.write(bArr, 0, read);
                        }
                        fileOutputStream.close();
                    }
                }
                zipInputStream.closeEntry();
                zipInputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (new File(Constants.MC_DIR + "/assets").exists()) {
            API_V1.setFinishedDownloading(true);
        }

        // Add the observer to the API_V1 class
        API_V1.addObserver(this);

        return super.onMainCreate(activity);
    }

    public static File newFile(File file, ZipEntry zipEntry) throws IOException {
        File file2 = new File(file, zipEntry.getName());
        String canonicalPath = file.getCanonicalPath();
        String canonicalPath2 = file2.getCanonicalPath();
        if (canonicalPath2.startsWith(canonicalPath + File.separator)) {
            return file2;
        }
        throw new IOException("Entry is outside of the target dir: " + zipEntry.getName());
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "pojlib";
    }

    @NonNull
    @Override
    public Set<SignalInfo> getPluginSignals() {
        // Register all signals to godot
        Set<SignalInfo> signals = new ArraySet<>();

        signals.add(new SignalInfo("finished_downloading", Boolean.class));
        signals.add(new SignalInfo("current_download", String.class));
        signals.add(new SignalInfo("msa_message", String.class));
        signals.add(new SignalInfo("ignore_instance_name", Boolean.class));
        signals.add(new SignalInfo("custom_ram_value", Boolean.class));
        signals.add(new SignalInfo("download_status", Float.class));
        signals.add(new SignalInfo("profile_image", String.class));
        signals.add(new SignalInfo("profile_name", String.class));
        signals.add(new SignalInfo("memory_value", String.class));
        signals.add(new SignalInfo("developer_mods", Boolean.class));
        signals.add(new SignalInfo("advanced_debugger", Boolean.class));

        return signals;
    }

    @UsedByGodot
    public Dictionary getMinecraftVersions() {
         return minecraftVersionsToDictionary(API_V1.getMinecraftVersions());
    }

    @UsedByGodot
    public Dictionary createNewInstance(String instanceName, Dictionary minecraftVersion) throws IOException {
        return minecraftInstanceToDictionary(API_V1.createNewInstance(getActivity(), instanceName, Constants.USER_HOME, dictionaryToMinecraftVersion(minecraftVersion)));
    }

    public Dictionary load(String instanceName) {
        return minecraftInstanceToDictionary(API_V1.load(instanceName, Constants.USER_HOME));
    }

    @UsedByGodot
    public void launchInstance(String instanceName, String client_id) {
        MinecraftInstance instance = API_V1.load(instanceName, Constants.USER_HOME);
        MinecraftAccount account = API_V1.login(client_id, getActivity());

        API_V1.launchInstance(getActivity(), account, instance);
    }

    @UsedByGodot
    public Dictionary fetchSavedLogin(String client_id) throws IOException, JSONException {
        // Convert MinecraftAccount to org.godotengine.godot.Dictionary for Godot
        return minecraftAccountToDictionary(API_V1.fetchSavedLogin(client_id, Constants.USER_HOME));
    }

    @UsedByGodot
    public boolean logout() {
        return API_V1.logout(Constants.USER_HOME);
    }

    @UsedByGodot
    public Dictionary login(String client_id) throws IOException, JSONException {
        // Convert MinecraftAccount to org.godotengine.godot.Dictionary for Godot
        return minecraftAccountToDictionary(API_V1.login(client_id, getActivity()));
    }

    private Dictionary minecraftAccountToDictionary(MinecraftAccount account) {
        if(account == null) return null;

        Dictionary minecraftAccount = new Dictionary();

        minecraftAccount.put("username", account.username);
        minecraftAccount.put("uuid", account.uuid);
        minecraftAccount.put("accessToken", account.accessToken);
        minecraftAccount.put("msaRefreshToken", account.msaRefreshToken);
        minecraftAccount.put("expiresIn", account.expiresIn);

        return minecraftAccount;
    }

    private Dictionary minecraftInstanceToDictionary(MinecraftInstance instance) {
        if(instance == null) return null;

        Dictionary minecraftInstance = new Dictionary();

        minecraftInstance.put("assetIndex", instance.assetIndex);
        minecraftInstance.put("versionName", instance.versionName);
        minecraftInstance.put("classpath", instance.classpath);
        minecraftInstance.put("mainClass", instance.mainClass);
        minecraftInstance.put("assetsDir", instance.assetsDir);
        minecraftInstance.put("versionType", instance.versionType);
        minecraftInstance.put("gameDir", instance.gameDir);

        return minecraftInstance;
    }

    private Dictionary minecraftVersionsToDictionary(MinecraftMeta.MinecraftVersion[] versions) {
        if(versions == null) return null;

        Dictionary minecraftVersions = new Dictionary();

        for (MinecraftMeta.MinecraftVersion version : versions) {
            minecraftVersions.put(version.id, minecraftVersionToDictionary(version));
        }

        return minecraftVersions;
    }

    private Dictionary minecraftVersionToDictionary(MinecraftMeta.MinecraftVersion version) {
        if(version == null) return null;

        Dictionary minecraftVersion = new Dictionary();

        minecraftVersion.put("id", version.id);
        minecraftVersion.put("sha1", version.sha1);

        return minecraftVersion;
    }

    private MinecraftMeta.MinecraftVersion dictionaryToMinecraftVersion(Dictionary dictionary) {
        if(dictionary == null) return null;

        MinecraftMeta.MinecraftVersion version = new MinecraftMeta.MinecraftVersion();

        version.id = (String) dictionary.get("id");
        version.sha1 = (String) dictionary.get("sha1");

        return version;
    }

    @Override
    public void onStaticPropertyChanged(StaticProperty property) {
        switch (property) {
            case FINISHED_DOWNLOADING:
                emitSignal("finished_downloading", API_V1.isFinishedDownloading());
                break;
            case CURRENT_DOWNLOAD:
                emitSignal("current_download", API_V1.getCurrentDownload());
                break;
            case MSA_MESSAGE:
                emitSignal("msa_message", API_V1.getMsaMessage());
                break;
            case IGNORE_INSTANCE_NAME:
                emitSignal("ignore_instance_name", API_V1.isIgnoreInstanceName());
                break;
            case CUSTOM_RAM_VALUE:
                emitSignal("custom_ram_value", API_V1.isCustomRAMValue());
                break;
            case DOWNLOAD_STATUS:
                emitSignal("download_status", (float)API_V1.getDownloadStatus());
                break;
            case PROFILE_IMAGE:
                emitSignal("profile_image", API_V1.getProfileImage());
                break;
            case PROFILE_NAME:
                emitSignal("profile_name", API_V1.getProfileName());
                break;
            case MEMORY_VALUE:
                emitSignal("memory_value", API_V1.getMemoryValue());
                break;
            case DEVELOPER_MODS:
                emitSignal("developer_mods", API_V1.isDeveloperMods());
                break;
            case ADVANCED_DEBUGGER:
                emitSignal("advanced_debugger", API_V1.isAdvancedDebugger());
                break;
        }
    }
}
