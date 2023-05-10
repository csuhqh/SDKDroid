package com.csulib.visitors;

import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Optional;

public class JarAPICollectVisitor extends VoidVisitorAdapter<Void> {
    private String currentPkg = null;
    private JSONArray methods = null;
    //更新最后的结果
    private JSONObject result = null;
    public JarAPICollectVisitor(JSONObject jsonObject){
        result = jsonObject;
    }



    @Override
    public void visit(MethodDeclaration n, Void arg) {

        //方法全名
        StringBuilder fullName = new StringBuilder();
        // 获取方法所在的类名
        Optional<ClassOrInterfaceDeclaration> ancestor = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if(ancestor.isPresent() && ancestor.get().getFullyQualifiedName().isPresent()){
            fullName.append(ancestor.get().getFullyQualifiedName().get());
            fullName.append(".");
            fullName.append(n.getSignature());
            fullName.append(";");  //方法名字
            NodeList<Parameter> parameters = n.getParameters();
            for(Parameter para: parameters){
                fullName.append(para.toString() + "|");
            }
            if (fullName.charAt(fullName.length() - 1) == ','){
                fullName.deleteCharAt(fullName.length() - 1);
            }
            fullName.append(";");
            fullName.append(n.getTypeAsString()); // 返回值
            fullName.append(";");
            fullName.append(ancestor.get().getFullyQualifiedName().get());
            fullName.append(";");
            fullName.append(currentPkg);
            System.out.println(fullName);
        }else {
            //忽略枚举类型
            Optional<EnumDeclaration> enumD = n.findAncestor(EnumDeclaration.class);
            if(enumD.isPresent())
                return;
            throw new RuntimeException();
        }
        methods.put(fullName.toString());
    }

    @Override
    public void visit(PackageDeclaration n, Void arg) { //第二执行
        currentPkg = n.getNameAsString();
        if(!result.has(currentPkg)){
            methods = new JSONArray();
            result.put(currentPkg, methods);
        }else {
            methods = result.getJSONArray(currentPkg);
        }

        super.visit(n, arg);
    }

}
