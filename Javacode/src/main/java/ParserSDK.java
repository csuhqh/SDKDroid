
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import com.csulib.utils.ASTUtils;
import org.json.JSONObject;

public class ParserSDK {

    public static void main(String[] args) throws IOException {
        JSONObject config = new JSONObject(new String(Files.readAllBytes(Paths.get("config.json"))));
        int version = config.getInt("android-version");
        String sourcesDir = "D:\\AndroidEnvs\\SDKEnvs\\sources";
        String platformDir = "D:\\AndroidEnvs\\SDKEnvs\\platforms";
        ASTUtils.parserAllJava(sourcesDir, platformDir, version);
    }
}