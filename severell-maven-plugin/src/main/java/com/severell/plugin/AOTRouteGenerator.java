package com.severell.plugin;

import com.severell.core.config.Config;
import com.severell.core.container.Container;
import com.severell.core.providers.ServiceProvider;
import com.severell.plugin.internal.ClassFileCompiler;
import com.severell.plugin.internal.RouteFileBuilder;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class is responsible for building the RouteBuilder.java and compiling it.
 * It has to be done after a compile otherwise we don't have access to the Providers and Container.
 */
@Mojo(name="generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class AOTRouteGenerator extends AbstractMojo {

    @Parameter(defaultValue = "${project}")
    public MavenProject project;

    @Parameter
    public String basePackage;

    @Parameter(readonly = true, defaultValue = "${project.build.directory}/classes")
    public String targetDirectory;

    public void bootstrap(Container c, ProjectClassLoader classLoader) {
        try {
            Config.setDir(project.getBuild().getOutputDirectory());
            Config.loadConfig();
        }catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }

        c.singleton("_MiddlewareList", classLoader.getMiddleware());
        try {
            c.singleton(classLoader.loadClass(basePackage + ".auth.Auth"), classLoader.initClass(classLoader.loadClass(basePackage + ".auth.Auth")));
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
            e.printStackTrace();
        }

        List<ServiceProvider> providers = Arrays.stream(classLoader.getProviders(c))
                .filter(s -> {
                    return !s.getClass().getSimpleName().equals("AppProvider")
                            && !s.getClass().getSimpleName().equals("JettyProvider")
                            && !s.getClass().getSimpleName().equals("RouteProvider");
                })
                .collect(Collectors.toList());

        for(ServiceProvider provider : providers) {
            try {
                provider.register();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        classLoader.initRoutes();

        for(ServiceProvider provider : providers) {
            try {
                provider.boot();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            Container container = new Container();
            ProjectClassLoader classLoader = new ProjectClassLoader(project, basePackage, getLog());
            Path target = Path.of(targetDirectory);
            bootstrap(container, classLoader);

            Path sourceFile = RouteFileBuilder.build(container, basePackage);

            String fileLocation = basePackage.replaceAll("\\.", "/");
            String[] files = new String[]{sourceFile.toString() + "/" + fileLocation + "/RouteBuilder.java"};
            boolean result = ClassFileCompiler.compile(files, project.getCompileClasspathElements(), target, project.getProperties());

            if(result) {
                getLog().info("Succesfully Compiled Routes");
            }

            recursiveDeleteOnExit(sourceFile);

        } catch (DependencyResolutionRequiredException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private static void recursiveDeleteOnExit(Path path) throws IOException {
        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file,
                                             @SuppressWarnings("unused") BasicFileAttributes attrs) {
                file.toFile().deleteOnExit();
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult preVisitDirectory(Path dir,
                                                     @SuppressWarnings("unused") BasicFileAttributes attrs) {
                dir.toFile().deleteOnExit();
                return FileVisitResult.CONTINUE;
            }
        });
    }

}
