package com.ardley.cedar.jaxrs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Scans classpath for Cedar authorization annotations to build action-to-resource-type mappings.
 *
 * <p>This scanner finds all methods annotated with @RequiresActions and @CedarResource
 * to automatically determine which actions apply to which resource types.
 */
public class ClasspathAnnotationScanner {

    private static final Logger log = LoggerFactory.getLogger(ClasspathAnnotationScanner.class);

    /**
     * Result of scanning classpath for Cedar annotations.
     */
    public static class ScanResult {
        private final Map<String, Set<String>> actionToResourceTypes;
        private final Set<String> allActions;

        /**
         * Constructs a scan result.
         *
         * @param actionToResourceTypes Map of actions to their resource types
         * @param allActions Set of all discovered actions
         */
        public ScanResult(Map<String, Set<String>> actionToResourceTypes, Set<String> allActions) {
            this.actionToResourceTypes = Collections.unmodifiableMap(actionToResourceTypes);
            this.allActions = Collections.unmodifiableSet(allActions);
        }

        /**
         * Get mapping of action name to the resource types it applies to.
         * Actions with empty sets are context-based (no specific resource).
         *
         * @return Map of action names to sets of resource types
         */
        public Map<String, Set<String>> getActionToResourceTypes() {
            return actionToResourceTypes;
        }

        /**
         * Get all discovered actions.
         *
         * @return Set of all action names
         */
        public Set<String> getAllActions() {
            return allActions;
        }
    }

    /**
     * Scans the specified package for Cedar annotations.
     *
     * @param packageName Base package to scan (e.g., "com.myapp.api")
     * @return ScanResult with action-to-resource-type mappings
     */
    public ScanResult scan(String packageName) {
        Map<String, Set<String>> actionToResourceTypes = new HashMap<>();
        Set<String> allActions = new HashSet<>();

        try {
            Set<Class<?>> classes = findClassesInPackage(packageName);
            log.debug("Found {} classes to scan in package: {}", classes.size(), packageName);

            for (Class<?> clazz : classes) {
                scanClass(clazz, actionToResourceTypes, allActions);
            }

            log.info("Scanned {} classes, found {} actions", classes.size(), allActions.size());

        } catch (Exception e) {
            log.error("Failed to scan package: " + packageName, e);
        }

        return new ScanResult(actionToResourceTypes, allActions);
    }

    /**
     * Scans specific classes for Cedar annotations (useful for testing).
     *
     * @param classes Classes to scan
     * @return ScanResult with action-to-resource-type mappings
     */
    public ScanResult scanClasses(Class<?>... classes) {
        Map<String, Set<String>> actionToResourceTypes = new HashMap<>();
        Set<String> allActions = new HashSet<>();

        for (Class<?> clazz : classes) {
            scanClass(clazz, actionToResourceTypes, allActions);
        }

        log.info("Scanned {} classes, found {} actions", classes.length, allActions.size());
        return new ScanResult(actionToResourceTypes, allActions);
    }

    private void scanClass(Class<?> clazz, Map<String, Set<String>> actionToResourceTypes, Set<String> allActions) {
        for (Method method : clazz.getDeclaredMethods()) {
            // Check for @RequiresActions (context-based)
            RequiresActions requiresActions = method.getAnnotation(RequiresActions.class);
            if (requiresActions != null) {
                for (String action : requiresActions.value()) {
                    allActions.add(action);
                    // Context-based actions have no specific resource type (empty set)
                    actionToResourceTypes.putIfAbsent(action, new HashSet<>());
                    log.debug("Found context-based action: {} in {}.{}",
                        action, clazz.getSimpleName(), method.getName());
                }
            }

            // Check parameters for @CedarResource (resource-based)
            for (Parameter parameter : method.getParameters()) {
                CedarResource cedarResource = parameter.getAnnotation(CedarResource.class);
                if (cedarResource != null) {
                    String resourceType = cedarResource.type();
                    for (String action : cedarResource.actions()) {
                        allActions.add(action);
                        actionToResourceTypes
                            .computeIfAbsent(action, k -> new HashSet<>())
                            .add(resourceType);
                        log.debug("Found resource-based action: {} applies to {} in {}.{}",
                            action, resourceType, clazz.getSimpleName(), method.getName());
                    }
                }
            }
        }
    }

    private Set<Class<?>> findClassesInPackage(String packageName) throws Exception {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = packageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);

        Set<Class<?>> classes = new HashSet<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            try {
                File directory = new File(resource.toURI());
                if (directory.exists() && directory.isDirectory()) {
                    classes.addAll(findClasses(directory, packageName));
                }
            } catch (Exception e) {
                log.debug("Could not process resource: {}", resource, e);
            }
        }

        return classes;
    }

    private Set<Class<?>> findClasses(File directory, String packageName) {
        Set<Class<?>> classes = new HashSet<>();

        if (!directory.exists()) {
            return classes;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return classes;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                try {
                    Class<?> clazz = Class.forName(className);
                    classes.add(clazz);
                } catch (ClassNotFoundException | NoClassDefFoundError e) {
                    log.trace("Could not load class: {}", className, e);
                }
            }
        }

        return classes;
    }
}
