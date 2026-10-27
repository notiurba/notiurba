import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.LoaderOptions;

/** Local syntax/path checks only. Does not certify a Render deployment. */
public final class ValidationConfigurationCheck {
    private static int checks;
    private static final Yaml YAML = new Yaml(new SafeConstructor(new LoaderOptions()));
    @SuppressWarnings("unchecked")
    static Map<String,Object> map(Object o) { return (Map<String,Object>)o; }
    static Map<String,Object> load(String path) throws Exception {
        try (var input=Files.newInputStream(Path.of(path))) { return map(YAML.load(input)); }
    }
    static void require(boolean ok,String name) {
        if (!ok) throw new AssertionError(name); checks++;
    }
    public static void main(String[] args) throws Exception {
        var render=load("render.yaml");
        var service=map(((List<?>)render.get("services")).get(0));
        Path root=Path.of(service.get("rootDir").toString()).toAbsolutePath().normalize();
        require(root.equals(Path.of("backend").toAbsolutePath().normalize()),"backend root");
        require(Files.isRegularFile(root.resolve(service.get("dockerfilePath").toString())),"Dockerfile exists");
        require(root.resolve(service.get("dockerContext").toString()).normalize().equals(root),"backend context");
        require(Files.isRegularFile(root.resolve("pom.xml")),"context contains pom");
        require("free".equals(service.get("plan")),"free plan");
        require("off".equals(service.get("autoDeployTrigger")),"autodeploy disabled string");
        require("docker".equals(service.get("runtime")),"docker runtime");
        String docker=Files.readString(root.resolve("Dockerfile"));
        require(docker.contains("temurin-21") && docker.contains("21-jre"),"Java 21 stages");
        var app=load("backend/src/main/resources/application.yml");
        require("${PORT:8080}".equals(map(app.get("server")).get("port")),"PORT environment");
        var validation=map(load("backend/src/main/resources/application-validation.yml").get("spring"));
        require(Boolean.FALSE.equals(map(validation.get("flyway")).get("enabled")),"Flyway disabled");
        require("validate".equals(map(map(validation.get("jpa")).get("hibernate")).get("ddl-auto")),"schema validation enabled");
        require(Boolean.TRUE.equals(map(map(validation.get("datasource")).get("hikari")).get("read-only")),"read-only pool");
        var workflow=load(".github/workflows/edt51-validation.yml");
        require(map(workflow.get("on")).containsKey("pull_request"),"pull request trigger");
        var jobs=map(workflow.get("jobs"));
        require(jobs.containsKey("backend-unit") && jobs.containsKey("migration-postgres18"),"separate jobs");
        for (Object job : jobs.values()) {
            var steps=(List<?>)map(job).get("steps");
            boolean java21=false, reports=false;
            for(Object step : steps) {
                var m=map(step);
                if (m.getOrDefault("uses","").toString().startsWith("actions/setup-java@"))
                    java21="21".equals(map(m.get("with")).get("java-version"));
                if (m.getOrDefault("uses","").toString().startsWith("actions/upload-artifact@"))
                    reports="always()".equals(m.get("if"));
            }
            require(java21 && reports,"Java 21 and reports on failure");
        }
        var factory=DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl",true);
        var pom=factory.newDocumentBuilder().parse("backend/pom.xml");
        require("21".equals(pom.getElementsByTagName("java.version").item(0).getTextContent()),"POM Java 21");
        String xml=Files.readString(Path.of("backend/pom.xml"));
        require(xml.contains("**/*IntegrationTest.java") && xml.contains("<id>migration-tests</id>")
            && xml.contains("<id>spatial-tests</id>") && xml.contains("<id>cloudinary-tests</id>"),"explicit external profiles");
        System.out.println("UTC="+java.time.Instant.now()+" configuration_checks="+checks+" failed=0");
    }
}
