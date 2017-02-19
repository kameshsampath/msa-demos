package org.workspace7.msa.calculator;

import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

/**
 * @author kameshs
 */
@RestController
@RequestMapping("/api")
public class SimpleCalculatorController {

    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleCalculatorController.class);

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/whoami", produces = "text/plain")
    @ApiOperation("Says whoami ")
    public String whoami() {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        return String.format("I am served from Host: %s", hostname);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/add/{num1}/{num2}", produces = "text/plain")
    @ApiOperation("Adds two numbers passed as path parameters")
    public String add(@PathVariable("num1") Integer num1, @PathVariable("num2") Integer num2) {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        int result = (num1 + num2);
        LOGGER.info("Product Result:{} executed on Pod {}",result,hostname);
        return String.format("Service Host :%s \n %d + %d = %d", hostname, num1, num2, result);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.GET, value = "/sub/{num1}/{num2}", produces = "text/plain")
    @ApiOperation("Subtracts two numbers passed as path parameters")
    public String sub(@PathVariable("num1") Integer num1, @PathVariable("num2") Integer num2) {
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        int result = (num1 - num2);
        LOGGER.info("Product Result:{} executed on Pod {}",result,hostname);
        return String.format("Service Host :%s \n %d - %d = %d", hostname, num1, num2, result);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, value = "/mul", produces = "text/plain",
            consumes = "application/json")
    @ApiOperation("Multiplies two numbers passed as body json, only int/long supported")
    public String mul(@RequestBody String numbersJson) {
        LOGGER.debug("Request : {}", numbersJson);
        long product = 1;
        if (numbersJson != null && !StringUtils.isEmpty(numbersJson)) {
            JSONObject jsonObject = new JSONObject(numbersJson);
            JSONArray jsonArray = jsonObject.getJSONArray("numbers");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    product = product * jsonArray.getLong(i);
                }
            }
        }
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        LOGGER.info("Product Result:{} executed on Pod {}",product,hostname);
        return String.format("Service Host :%s \n Product  = %d", hostname, product);
    }

    @CrossOrigin
    @RequestMapping(method = RequestMethod.POST, value = "/div", produces = "text/plain",
            consumes = "application/json")
    @ApiOperation("Divides two numbers passed as json, all answer is returned as double")
    public String div(@RequestBody String numbersJson) {
        LOGGER.debug("Request : {}", numbersJson);
        String hostname = System.getenv().getOrDefault("HOSTNAME", "Unknown");
        double answer = 0;
        if (numbersJson != null && !StringUtils.isEmpty(numbersJson)) {
            JSONObject jsonObject = new JSONObject(numbersJson);
            JSONArray jsonArray = jsonObject.getJSONArray("numbers");
            if (jsonArray != null && jsonArray.length() > 0) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    double n = jsonArray.getDouble(i);
                    if (n != 0) {
                        if (answer == 0) {
                            answer = n;
                        } else {
                            answer = answer / n;
                        }
                    } else {
                        answer = 0;
                        LOGGER.error("Exception Divide by 0");
                        break;
                    }
                }
            }
        }
        LOGGER.info("Divide Result:{} executed on Pod {}",answer,hostname);
        return String.format("Service Host :%s \n Answer = %f",hostname, answer);
    }
}
