package com.csulib.visitors;

import com.github.javaparser.ast.ImportDeclaration;
import com.github.javaparser.ast.Modifier;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.PackageDeclaration;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.BlockStmt;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.ReferenceType;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.UnsolvedSymbolException;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;

public class APIVisitor extends VoidVisitorAdapter<Void> {
    private String currentPkg = null;
    private String currentCls = null;
    private JSONObject info = null;
    private JSONObject imports = new JSONObject();
    private HashSet<String> hashSet = null;

    //更新最后的结果
    private JSONObject result = null;
    private JSONObject jarAPIs = null;

    public APIVisitor(JSONObject result, JSONObject jarAPIs){
        this.result = result;
        this.jarAPIs = jarAPIs;
    }


    private String dealUnsolved(String scope){
        String remain = "";
        String head = scope;
        String result = null;
        if(scope.contains(".")){
            remain = scope.substring(scope.indexOf(".")); // a.b.c  ==> .b.c
            head = scope.replace(remain, ""); // a.b.c ==> a
        }
        if(imports.has(head)){ //其他包的framework导入
            result =  imports.getString(head) + remain + ".framework";
        }else {//本包的framework导入
            result =  currentPkg + "." + head + remain + ".framework";
        }
        System.err.println("dealUnsolved: " + scope + "==>" + result);
        return result;
    }


    @Override
    public void visit(MethodDeclaration n, Void arg) {

        hashSet = new HashSet<>();
        info = new JSONObject();
        JSONArray throwType = new JSONArray();
        JSONArray calls = new JSONArray();
        JSONArray simpleCalls = new JSONArray();
        JSONArray refers = new JSONArray();
        JSONArray permissions = new JSONArray();
        JSONObject classInfo = new JSONObject();
        JSONArray implementType = new JSONArray();
        JSONArray extendType = new JSONArray();
        JSONArray paramsType = new JSONArray();
        JSONArray paramsName = new JSONArray();
        info.put("throws", throwType);
        info.put("simpleCalls", simpleCalls);
        info.put("calls", calls);
        info.put("refers", refers);
        info.put("permissions", permissions);
        info.put("classInfo", classInfo);
        info.put("paramsType", paramsType);
        info.put("paramsName", paramsName);
        classInfo.put("implements", implementType);
        classInfo.put("extendType", extendType);
        classInfo.put("packageName", currentPkg);
        String cls_name = "";
        //方法全名
        StringBuilder fullName = new StringBuilder();
        // 获取方法所在的类名
        Optional<ClassOrInterfaceDeclaration> ancestor = n.findAncestor(ClassOrInterfaceDeclaration.class);
        if(ancestor.isPresent() && ancestor.get().getFullyQualifiedName().isPresent()){
            cls_name = ancestor.get().getFullyQualifiedName().get();
            //是否是jar文件中的
            boolean isApi = false;
            if(!jarAPIs.has(currentPkg))
                return;
            for (Object o : jarAPIs.getJSONArray(currentPkg)) {
                String api = (String) o;
                api = api.split(";")[0];
                if (api.equals(cls_name + "." + n.getSignature())) {
                    isApi = true;
                    System.out.println("api:" + api);
                    break;
                }
            }
            if(! isApi){  //不是API不处理
                System.out.println("====: "+ cls_name + "." + n.getSignature());
                return;
            }
            fullName.append(cls_name);
            classInfo.put("classname", cls_name);
            //当前的类名
            System.out.println("classname: " + cls_name);
            NodeList<ClassOrInterfaceType> implementedTypes = ancestor.get().getImplementedTypes();
            for(ClassOrInterfaceType cit: implementedTypes){
                System.out.printf("解析实现:"+cit);
                String tempImplementType = "";
                try{
                    tempImplementType = cit.resolve().describe();
                }catch (UnsolvedSymbolException e){
                    tempImplementType = dealUnsolved(cit.getNameWithScope());
                }
                implementType.put(tempImplementType);
                System.out.println("Implements: " + cit + "==>" + tempImplementType);
            }
            if(ancestor.get().getExtendedTypes().size() != 0){
                ClassOrInterfaceType cit = ancestor.get().getExtendedTypes(0);
                System.out.println("解析继承:" + cit);
                String tempExtendType = "";
                try{
                    tempExtendType = cit.resolve().describe();
                }catch (UnsolvedSymbolException e){
                    tempExtendType = dealUnsolved(cit.getNameWithScope());
                }
                extendType.put(tempExtendType);
                System.out.println("Extends: " + cit + "==>" + tempExtendType);
            }
        }else{
            System.err.println("错误方法: " + n.getSignature());
            return;
        }
        //方法名
        fullName.append(".");
        fullName.append(n.getSignature());
        // 参数
//        fullName.append("(");
        NodeList<Parameter> parameters = n.getParameters();
        String paramType = "";
        for(Parameter param: parameters){
            String[] ps = param.toString().split(" ");
            paramsName.put(ps[ps.length - 1]);
            try{
                paramType = param.getType().resolve().describe();
            }catch (UnsolvedSymbolException e){
                paramType = dealUnsolved(param.getTypeAsString());
            }
            paramsType.put(paramType);
//            fullName.append(paramType);
//            fullName.append(",");
        }
//        if(parameters.size() != 0)
//            fullName.replace(fullName.length() - 1, fullName.length(), ")");
//        else
//            fullName.append(")");


//        methodInfo.put(fullName.toString(), info); //{"mehtodNmae":{}}
        result.put(fullName.toString(), info);
        //返回值
        String returnType = "";
        try{
            returnType = n.getType().resolve().describe();
        }catch (UnsolvedSymbolException e){

            returnType = dealUnsolved(n.getTypeAsString());
        }
        info.put("returnType", returnType);
        System.out.println("reteurnType: " + n.getTypeAsString() + "==>" + returnType);
        //outer-throws
        NodeList<ReferenceType> thrownExceptions = n.getThrownExceptions();
        String exceptionType = null;
        for(ReferenceType excep: thrownExceptions){
            try{
                exceptionType =  excep.resolve().describe();
            }catch (UnsolvedSymbolException e){
                exceptionType = dealUnsolved(excep.toString());
            }
            System.out.println("out-throws: " + excep + "==>" +exceptionType);
            if(hashSet.add(exceptionType))
                throwType.put(exceptionType);
        }

        //处理方法体 *****************************************************
        Optional<BlockStmt> body = n.getBody();
        if(body.isEmpty())
            return;
        BlockStmt b = body.get();

        System.out.println("***********处理方法:"+fullName);
        //获得方法中所有的变量
        List<NameExpr> nes = b.findAll(NameExpr.class); //类名，变量
        String nameExprType = "";
        for(NameExpr ne: nes){
            try{
                nameExprType = ne.calculateResolvedType().describe();
            }catch (UnsolvedSymbolException e){
                if(e.getName().contains(" "))
                    nameExprType = dealUnsolved(ne.getNameAsString());
                else
                    nameExprType = dealUnsolved(e.getName());
            }catch (Exception e){
                System.out.println("不可解:" + ne.toString());
                nameExprType = "Unresolved.Object";
//                System.exit(0);
            }
            if(nameExprType.contains("Exception") || nameExprType.contains("exception")){ //exception
                System.out.println("inner-throws: " + ne + "==>" +nameExprType);
                if(hashSet.add(nameExprType))
                    throwType.put(nameExprType);
            }else if(nameExprType.equals("java.lang.String")){ //判断是否是权限
                if(imports.has(ne.getNameAsString())){  //字段中有
                    if(imports.getString(ne.getNameAsString()).startsWith("android.Manifest.permission.") ){
                        if(hashSet.add(imports.getString(ne.getNameAsString()))){
                            permissions.put(imports.getString(ne.getNameAsString()));
                        }
                    }

                }
            }else{
                if(hashSet.add(nameExprType))
                    refers.put(nameExprType);
                System.out.println("NameExpr: " + ne + " ==> " + nameExprType);
            }

        }

        //修饰符
        JSONArray modifiers_arr = new JSONArray();
        NodeList<Modifier> modifiers = n.getModifiers();
        for(Modifier modifier: modifiers){
            modifiers_arr.put(modifier.toString().strip());
        }
        info.put("modifier", modifiers_arr);

        // call
        List<MethodCallExpr> methodCalls = b.findAll(MethodCallExpr.class);
        String fullCallName = "";
        for(MethodCallExpr mc: methodCalls){
            String caller = null;
            Optional<Expression> scope = mc.getScope();
            if(scope.isEmpty()){
                caller = cls_name;  //this
            }else {
                try {
                    caller = scope.get().calculateResolvedType().describe(); // common
                } catch (UnsolvedSymbolException e) {
                    if (e.getName().contains(" ")) {//完全不能解析
                        System.out.println("完全不能解析: " + scope.get());
                        caller = dealUnsolved(scope.get().toString());
                    } else {
                        caller = dealUnsolved(e.getName());
                    }
                } catch (Exception e) {
                    System.err.println("不能解析:" + scope.get());
                    continue;
                }
            }
            fullCallName = caller + "." + mc.getName() + "(";

            NodeList<Expression> arguments = mc.getArguments();
            String simpleFullCallName = fullCallName + arguments.size() + ")";
            if(hashSet.add(simpleFullCallName)){
                simpleCalls.put(simpleFullCallName);
            }
            for(Expression argu: arguments){
                System.out.println("解析参数: " + argu);
                try{
                    if(argu.toString().contains("?") || argu.toString().contains("At("))
                        fullCallName += "Unresolved.Object,";
                    else
                        fullCallName += argu.calculateResolvedType().describe() + ",";
                }catch (UnsolvedSymbolException e){
                    if(e.getName().contains(" "))
                        fullCallName += dealUnsolved(argu.toString()) + ",";
                    else
                        fullCallName += dealUnsolved(argu.toString()) + ",";
                }catch (Exception e){
                    fullCallName += "Unresolved.Object" + ",";
                }

            }
            if(fullCallName.endsWith(","))
                fullCallName = fullCallName.substring(0, fullCallName.length() - 1);
            fullCallName += ")";
            if(hashSet.add(fullCallName))
                calls.put(fullCallName);
            System.out.println("calls: "+ mc + " ==> " + fullCallName);
        }
        //权限
        //获得所有字面量
        List<StringLiteralExpr> all2 = b.findAll(StringLiteralExpr.class);
        for(StringLiteralExpr a: all2){
            if(a.toString().startsWith("android.permission.")){
                System.out.println("permission-LiteralExpr: " + a + "==>" + "permission");
                if(hashSet.add(a.toString()))
                    permissions.put(a.toString());
            } else
                System.out.println("permission-LiteralExpr: " + a + "==>" + "not permission");
        }
        //获得所有字段(有.形式的)
        List<FieldAccessExpr> all3 = b.findAll(FieldAccessExpr.class);
        for(FieldAccessExpr a: all3){
            System.out.println(a);
            String fieldAccess = "";
            try{ //android系统内的字段，舍弃
                fieldAccess = a.getScope().calculateResolvedType().describe()+ "." + a.getNameAsString();
            }catch (Exception e){
                System.err.println("UnsolvedSymbolException--field: " + a);
                continue;
            }
            if(fieldAccess.startsWith("android.Manifest.permission.")){
                System.out.println("permission-FieldAccessExpr:" + a + " ==> " + fieldAccess);
                if(hashSet.add(fieldAccess))
                    permissions.put(fieldAccess);
            } else
                System.out.println("permission-FieldAccessExpr:" + a + " ==> " + "no permission");
        }
    }
    public void visit(ImportDeclaration n, Void arg){
        String name = n.getNameAsString();
        imports.put(name.substring(name.lastIndexOf(".") + 1), name);
    }

    @Override
    public void visit(ClassOrInterfaceDeclaration n, Void arg) {
        //获取所有字段
        super.visit(n, arg);
    }

    @Override
    public void visit(PackageDeclaration n, Void arg) { //第二执行
        currentPkg = n.getNameAsString();
        super.visit(n, arg);
    }
}
