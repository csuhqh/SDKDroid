package com.csulib.utils;

import com.csulib.visitors.APIVisitor;
import com.csulib.visitors.JarAPICollectVisitor;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JarTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Optional;

public class ASTUtils {
    /**
     * 将AndroidJar文件进行反编译为java文件，进行分析
     */
    public static int Decompiled(String androidJar) throws IOException, InterruptedException {
        String projectDir = System.getProperty ("user.dir");
        String cfrjar = projectDir + File.separator + "Javacode" + File.separator +"res" + File.separator + "cfr-0.152.jar";
        String outputdir = projectDir + File.separator + "outputs" + File.separator + "temp";
        String cmds = "java -jar " + cfrjar + " " + androidJar + " " + "--outputdir " + outputdir;
        Process process = Runtime.getRuntime().exec(cmds);

        new Thread(() -> {
            try {
                InputStreamReader iserr = new InputStreamReader(process.getErrorStream());
                BufferedReader brerr = new BufferedReader(iserr);
                String line = null;
                while ((line = brerr.readLine()) != null); //将输出信息的读出，否则waitFor()会阻塞
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }).start();
        return process.waitFor();
    }

    public static JSONObject collectAPI(String platformDir, int version) throws IOException {
        platformDir += File.separator + "android-" + version + File.separator + "android.jar";  //jar文件
        File outputDir = new File("outputs" + File.separator + "android-" + version);
        if(outputDir.exists()){
            MyFileUtils.deleteFolder(outputDir);
        }
        outputDir.mkdir();
        String outputPath = outputDir.getPath() + File.separator + "androidJar-" + version + ".json";
        try{
            int state = Decompiled(platformDir);
            if(state != 0){
                throw new IOException("反编译android.jar文件失败");
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        ArrayList<File> files = new ArrayList<>();
        MyFileUtils.getAllFile(new File("outputs/temp"), files);
        JavaParser parser = null;
        JSONObject result = new JSONObject();

        for(File file: files){
            if(file.getPath().contains("annotation")){
                continue;
            }
            System.out.println("处理文件" + file.getPath());
            parser = new JavaParser();
            Optional<CompilationUnit> cu = parser.parse(file).getResult();
            if(cu.isPresent()){
                cu.get().accept(new JarAPICollectVisitor(result), null);
            }

        }
        MyFileUtils.saveResult(result, outputPath);
        MyFileUtils.deleteFolder(new File("outputs/temp"));
        return result;
    }

    public static void parserAllJava(String soureceDir, String platformDir, int version) throws IOException {
        //解析android.jar文件，获得方法列表
        JSONObject jarAPIs = collectAPI(platformDir, version);
        soureceDir += File.separator + "android-" + version;
        platformDir += File.separator + "android-" + version + File.separator + "android.jar";
        String output = "outputs/android-" + version + File.separator + "android-" +version + ".json";
        JSONObject result = new JSONObject();
        MyFileUtils.saveResult(result, output);
        CombinedTypeSolver solver = new CombinedTypeSolver();
        solver.add(new JavaParserTypeSolver(soureceDir));
        solver.add(new JarTypeSolver(platformDir));
        solver.add(new ReflectionTypeSolver());
        JavaSymbolSolver ts = new JavaSymbolSolver(solver);
        ParserConfiguration parserConfiguration = new ParserConfiguration();
        parserConfiguration.setSymbolResolver(ts);
        ArrayList<File> allFiles = new ArrayList<>();
        MyFileUtils.getAllFile(new File(soureceDir), allFiles);

        JavaParser parser = null;
        Optional<CompilationUnit> cu = null;
        int i = 0;
        int countAll = allFiles.size();
        for(File fi: allFiles){
            parser = new JavaParser(parserConfiguration);
            System.out.println("*******************************处理文件" + fi.getPath());
            System.err.println("progress: " + i + "/" + countAll + "---" +  i * 100/countAll + "%");
            cu = parser.parse(fi).getResult();
            if(cu.isPresent()){
                cu.get().accept(new APIVisitor(result, jarAPIs), null);

            }
            i += 1;
        }
        MyFileUtils.saveResult(result, output);

    }



}
