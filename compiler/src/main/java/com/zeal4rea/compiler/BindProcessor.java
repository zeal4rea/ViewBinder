package com.zeal4rea.compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.zeal4rea.annotation.BindActivity;
import com.zeal4rea.annotation.BindView;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

@AutoService(Processor.class)
public class BindProcessor extends AbstractProcessor {
    private Elements elementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        elementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        System.out.println("BindProcessor:process");
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindActivity.class);
        for (Element element : elements) {
            TypeElement typeElement = (TypeElement) element;
            List<? extends Element> members = elementUtils.getAllMembers(typeElement);
            MethodSpec.Builder bindViewMethodBuilder = MethodSpec.methodBuilder("bindView")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(TypeName.VOID)
                    .addParameter(ClassName.get(typeElement.asType()), "activity");
            for (Element item : members) {
                BindView bindView = item.getAnnotation(BindView.class);
                if (bindView == null) continue;
                bindViewMethodBuilder.addStatement(String.format("activity.%s = (%s) activity.findViewById(%s)", item.getSimpleName(), ClassName.get(item.asType()), bindView.value()));
            }
            TypeSpec typeSpec = TypeSpec.classBuilder("Bind" + element.getSimpleName())
                    .superclass(TypeName.get(typeElement.asType()))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addMethod(bindViewMethodBuilder.build())
                    .build();
            JavaFile javaFile = JavaFile.builder(getPackageName(typeElement), typeSpec).build();
            try {
                javaFile.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton(BindActivity.class.getCanonicalName());
    }

    private String getPackageName(TypeElement typeElement) {
        return elementUtils.getPackageOf(typeElement).getQualifiedName().toString();
    }
}
