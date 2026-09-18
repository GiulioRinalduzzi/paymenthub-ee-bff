package org.apache.fineract.config.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * The property names in this service are not the ones a record would produce on its own: one key has
 * an underscore inside a dotted name, one is a Java keyword, one had a capital letter in the middle,
 * and the deployment sets two of them as environment variables with a dash in the name.
 *
 * <p>
 * These tests bind from the environment of the running gazelle deployment, written exactly as the CR
 * writes it, including the values it deliberately leaves empty. If a rename ever creeps in, the build
 * says so instead of a deployment going quiet.
 * </p>
 */
class DeploymentEnvironmentBindingTest {

    /** The environment the ph-ee-operations-app deployment gives the pod, verbatim. */
    private static Binder deploymentEnvironment() {
        Map<String, Object> variables = new LinkedHashMap<>();
        variables.put("FINERACT_DATASOURCE_CORE_USERNAME", "mifos");
        variables.put("FINERACT_DATASOURCE_CORE_PASSWORD", "from-the-secret");
        variables.put("FINERACT_DATASOURCE_CORE_HOST", "operationsmysql");
        variables.put("FINERACT_DATASOURCE_CORE_PORT", "3306");
        variables.put("FINERACT_DATASOURCE_CORE_SCHEMA", "tenants");
        variables.put("TOKEN_CLIENT_CHANNEL_SECRET", "");
        variables.put("CLOUD_AWS_S3BASEURL", "http://minio:9000");
        variables.put("APPLICATION_BUCKET-NAME", "paymenthub-ee");
        variables.put("CLOUD_AWS_REGION_STATIC", "");
        variables.put("CLOUD_AWS_CREDENTIALS_ACCESS-KEY", "");
        variables.put("CLOUD_AWS_CREDENTIALS_SECRET-KEY", "");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, variables));
        ConfigurationPropertySources.attach(environment);
        return Binder.get(environment);
    }

    @Test
    void bindsTheDatasourceTheDeploymentSets() {
        FineractDatasourceProperties properties = deploymentEnvironment()
                .bind("fineract.datasource", FineractDatasourceProperties.class).get();

        assertEquals("operationsmysql", properties.core().host());
        assertEquals(3306, properties.core().port());
        assertEquals("tenants", properties.core().schema());
        assertEquals("mifos", properties.core().username());
        assertEquals("from-the-secret", properties.core().password());
    }

    @Test
    void keepsTheUnderscoreInDriverclassName() {
        // fineract.datasource.common.driverclass_name is the name in application.yml
        FineractDatasourceProperties properties = Binder.get(environmentOf("fineract.datasource.common.driverclass_name",
                "com.mysql.cj.jdbc.Driver")).bind("fineract.datasource", FineractDatasourceProperties.class).get();

        assertEquals("com.mysql.cj.jdbc.Driver", properties.common().driverclassName());
    }

    @Test
    void acceptsTheChannelSecretTheDeploymentLeavesEmpty() {
        // TOKEN_CLIENT_CHANNEL_SECRET is declared with no value in the deployment. Binding it must
        // not fail: an empty secret is a problem to fix in the deployment, not a reason to refuse to
        // start.
        TokenProperties properties = deploymentEnvironment().bind("token", TokenProperties.class).get();

        assertEquals("", properties.client().channel().secret());
        assertEquals(600, properties.user().accessValiditySeconds());
    }

    @Test
    void bindsTheBucketNameFromAnEnvironmentVariableWithADash() {
        ApplicationProperties properties = deploymentEnvironment().bind("application", ApplicationProperties.class).get();

        assertEquals("paymenthub-ee", properties.bucketName());
    }

    @Test
    void bindsTheAwsValuesIncludingTheEmptyOnesAndTheRenamedKey() {
        CloudProperties properties = deploymentEnvironment().bind("cloud", CloudProperties.class).get();

        // CLOUD_AWS_S3BASEURL matches the new s3-base-url spelling as well as the old one
        assertEquals("http://minio:9000", properties.aws().s3BaseUrl());
        // "static" is a Java keyword, so the component is staticRegion with @Name("static")
        assertEquals("", properties.aws().region().staticRegion());
        assertEquals("", properties.aws().credentials().accessKey());
        assertEquals("", properties.aws().credentials().secretKey());
    }

    @Test
    void bindsTheApplicationYmlSpellingOfTheAwsKeysToo() {
        CloudProperties properties = Binder.get(environmentOf("cloud.aws.s3-base-url", "https://s3.ap-south-1.amazonaws.com"))
                .bind("cloud", CloudProperties.class).get();

        assertEquals("https://s3.ap-south-1.amazonaws.com", properties.aws().s3BaseUrl());
    }

    @Test
    void buildsEveryGroupFromItsDefaultsWhenTheSectionIsMissing() {
        Binder empty = Binder.get(new StandardEnvironment());

        // @DefaultValue on the groups: a missing section is built from its defaults rather
        // than arriving null and failing later with a NullPointerException
        FineractDatasourceProperties datasource = empty.bindOrCreate("fineract.datasource", FineractDatasourceProperties.class);
        assertEquals("operations-mysql", datasource.core().host());
        assertEquals(3306, datasource.core().port());
        assertEquals("com.mysql.cj.jdbc.Driver", datasource.common().driverclassName());

        CloudProperties cloud = empty.bindOrCreate("cloud", CloudProperties.class);
        assertEquals("", cloud.aws().credentials().accessKey());
        assertEquals("", cloud.aws().region().staticRegion());
        assertEquals("", cloud.azure().blob().connectionString());

        TokenProperties token = empty.bindOrCreate("token", TokenProperties.class);
        assertEquals(43200, token.user().refreshValiditySeconds());
        assertEquals("", token.client().channel().secret());
    }

    private static StandardEnvironment environmentOf(String name, String value) {
        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put(name, value);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources()
                .addFirst(new org.springframework.core.env.MapPropertySource("test", properties));
        ConfigurationPropertySources.attach(environment);
        return environment;
    }
}
