package com.orange.oss.cloudfoundry.broker.opsautomation.ondemandbroker.pipeline;

import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;

/**
 * Created by ijly7474 on 18/12/17.
 */
public class StructureGeneratorHelperTest {

    @Test
    public void check_generated_path(){
            //Given a root path and path elements
            Path rootPath = Paths.get("/tmp");
            String element1 = "element1";
            String element2 = "element2";
            String element3 = "element3";

            //When
            Path path = StructureGeneratorHelper.generatePath(rootPath, element1, element2, element3);
            String actual = String.valueOf(path);

            //Then
            StringBuffer sb = new StringBuffer(String.valueOf(rootPath));
            sb.append(File.separator)
                    .append(element1).append(File.separator)
                    .append(element2).append(File.separator)
                    .append(element3);
            String expected = sb.toString();
            assertEquals(expected, actual);
    }

    @Test
    public void check_find_and_replace(){
        //Given a template with markers
        List<String> lines = new ArrayList<String>();
        lines.add("---");
        lines.add("deployment:");
        lines.add("  @service_instance@:");
        lines.add("  value: @service_instance@");
        lines.add("  value: @url@.((!/secrets/cloudfoundry_system_domain))");

        //When asking to replace some markers
        Map<String, String> map = new HashMap<String, String>();
        map.put("@service_instance@", "c_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        map.put("@url@", "cassandra-broker_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0");
        List<String> resultLines = StructureGeneratorHelper.findAndReplace(lines, map);

        //Then
        assertEquals("---", resultLines.get(0));
        assertEquals("deployment:", resultLines.get(1));
        assertEquals("  c_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0:", resultLines.get(2));
        assertEquals("  value: c_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0", resultLines.get(3));
        assertEquals("  value: cassandra-broker_aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa0.((!/secrets/cloudfoundry_system_domain))", resultLines.get(4));
    }
}
