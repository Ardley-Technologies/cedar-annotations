package com.ardley.cedar.jaxrs;

import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ClasspathAnnotationScannerTest {

    @Test
    void scan_shouldFindRequiresActionsAnnotations() {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult result = scanner.scanClasses(TestResources.class);

        assertTrue(result.getAllActions().contains("CreateUser"),
            "Should find CreateUser action. Found: " + result.getAllActions());
        assertTrue(result.getAllActions().contains("ListUsers"),
            "Should find ListUsers action. Found: " + result.getAllActions());

        // Context-based actions should have empty resource types
        Set<String> createUserResources = result.getActionToResourceTypes().get("CreateUser");
        assertNotNull(createUserResources);
        assertTrue(createUserResources.isEmpty(), "Context-based actions should have empty resource set");
    }

    @Test
    void scan_shouldFindCedarResourceAnnotations() {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult result = scanner.scanClasses(TestResources.class);

        assertTrue(result.getAllActions().contains("ViewUser"));
        assertTrue(result.getAllActions().contains("DeleteUser"));

        // Resource-based actions should have specific resource types
        Set<String> viewUserResources = result.getActionToResourceTypes().get("ViewUser");
        assertNotNull(viewUserResources);
        assertTrue(viewUserResources.contains("User"));

        Set<String> deleteUserResources = result.getActionToResourceTypes().get("DeleteUser");
        assertNotNull(deleteUserResources);
        assertTrue(deleteUserResources.contains("User"));
    }

    @Test
    void scan_shouldHandleMultipleResourceTypesForSameAction() {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult result = scanner.scanClasses(TestResources.class);

        assertTrue(result.getAllActions().contains("View"));

        // View action applies to both User and Document
        Set<String> viewResources = result.getActionToResourceTypes().get("View");
        assertNotNull(viewResources);
        assertTrue(viewResources.contains("User"));
        assertTrue(viewResources.contains("Document"));
        assertEquals(2, viewResources.size());
    }

    @Test
    void scan_shouldHandleMultipleActionsOnSameParameter() {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult result = scanner.scanClasses(TestResources.class);

        // Transfer action has multiple actions on same resource
        assertTrue(result.getAllActions().contains("TransferDocument"));
        assertTrue(result.getAllActions().contains("ViewDocument"));

        Set<String> transferResources = result.getActionToResourceTypes().get("TransferDocument");
        assertTrue(transferResources.contains("Document"));

        Set<String> viewDocResources = result.getActionToResourceTypes().get("ViewDocument");
        assertTrue(viewDocResources.contains("Document"));
    }

    @Test
    void scanResult_shouldBeImmutable() {
        ClasspathAnnotationScanner scanner = new ClasspathAnnotationScanner();
        ClasspathAnnotationScanner.ScanResult result = scanner.scanClasses(TestResources.class);

        assertThrows(UnsupportedOperationException.class, () -> {
            result.getAllActions().add("NewAction");
        });

        assertThrows(UnsupportedOperationException.class, () -> {
            result.getActionToResourceTypes().put("NewAction", Set.of());
        });
    }

    // Test resource classes with various annotation combinations

    public static class TestResources {

        @RequiresActions({"CreateUser"})
        public void createUser() {
            // Context-based action
        }

        @RequiresActions({"ListUsers"})
        public void listUsers() {
            // Context-based action
        }

        public void getUser(
            @PathParam("userId")
            @CedarResource(type = "User", actions = {"ViewUser"})
            String userId
        ) {
            // Resource-based action
        }

        public void deleteUser(
            @PathParam("userId")
            @CedarResource(type = "User", actions = {"DeleteUser"})
            String userId
        ) {
            // Resource-based action
        }

        public void viewUserOrDocument(
            @QueryParam("id")
            @CedarResource(type = "User", actions = {"View"})
            String userId
        ) {
            // View applies to User
        }

        public void viewDocument(
            @PathParam("docId")
            @CedarResource(type = "Document", actions = {"View"})
            String docId
        ) {
            // View also applies to Document
        }

        public void transferDocument(
            @PathParam("docId")
            @CedarResource(type = "Document", actions = {"TransferDocument", "ViewDocument"})
            String docId
        ) {
            // Multiple actions on same resource
        }
    }
}
