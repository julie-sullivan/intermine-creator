package io.swagger.api;

import io.swagger.configuration.MineUserConfigRepository;
import io.swagger.model.DataFile;
import io.swagger.model.DataFileProperties;
import io.swagger.model.DataFilePropertiesResponse;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.annotations.*;
import io.swagger.model.DataFilePropertiesResponseAnswers;
import org.intermine.configurator.DataFileManager;
import org.intermine.configurator.MineConfigManager;
import org.intermine.configurator.validation.ValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import javax.validation.constraints.*;
import javax.validation.Valid;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@javax.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2019-06-27T09:01:47.965Z[GMT]")
@Controller
public class FileApiController implements FileApi {

    private static final Logger log = LoggerFactory.getLogger(FileApiController.class);
    MineConfigManager mineConfigManager = new MineConfigManager();
    private final ObjectMapper objectMapper;

    private final HttpServletRequest request;

    @Autowired
    MineUserConfigRepository repository;

    @org.springframework.beans.factory.annotation.Autowired
    public FileApiController(ObjectMapper objectMapper, HttpServletRequest request) {
        this.objectMapper = objectMapper;
        this.request = request;

    }

    public ResponseEntity<DataFileProperties> detectFileProperties(@ApiParam(value = "File that needs to be identified." ,required=true )  @Valid @RequestBody DataFile body,@NotNull @ApiParam(value = "ID of user who owns this mine", required = true) @Valid @RequestParam(value = "userId", required = true) UUID userId,@NotNull @ApiParam(value = "ID of mine", required = true) @Valid @RequestParam(value = "mineId", required = true) UUID mineId) {
        String accept = request.getHeader("Accept");
        if (!mineConfigManager.isValid(repository, mineId, userId)) {
            throw new IllegalArgumentException("User or mine ID not found");
        }

        DataFile dataFile = (DataFile) body;
        UUID fileId = dataFile.getFileId();

        if (System.getenv("IM_DATA_DIR") == null) {
            throw new IllegalArgumentException("Please set ENV VAR '$IM_DATA_DIR'");
        }

        String fileLocation = DataFileManager.getFilePath(mineId.toString(), userId.toString(), fileId.toString(),
                System.getenv("IM_DATA_DIR"), dataFile.getName());

        ValidationResponse validationResponse = DataFileManager.processDataFile(dataFile, fileLocation);
        if (validationResponse.isValid) {
            return new ResponseEntity(validationResponse.dataFileProperties, HttpStatus.OK);
        } else {
            throw new IllegalArgumentException(validationResponse.errorMessage);
        }
    }

    public ResponseEntity<Void> saveFileProperties(@ApiParam(value = "File that needs to be identified." ,required=true )  @Valid @RequestBody DataFilePropertiesResponse body,@NotNull @ApiParam(value = "ID of mine to fetch", required = true) @Valid @RequestParam(value = "mineId", required = true) UUID mineId,@NotNull @ApiParam(value = "ID of user who owns this mine", required = true) @Valid @RequestParam(value = "userId", required = true) UUID userId) {
        String accept = request.getHeader("Accept");

        if (!mineConfigManager.isValid(repository, mineId, userId)) {
            throw new IllegalArgumentException("User or mine ID not found");
        }

        if (System.getenv("IM_DATA_DIR") == null) {
            throw new IllegalArgumentException("Please set ENV VAR '$IM_DATA_DIR'");
        }

        DataFile dataFile = (DataFile) body.getDataFile();
        DataFile.FileFormatEnum fileFormatEnum = dataFile.getFileFormat();
        UUID fileId = dataFile.getFileId();

        // TODO refactor this to be overridden in the unit tests.
        String fileLocation = DataFileManager.getFilePath(mineId.toString(), userId.toString(), fileId.toString(),
                System.getenv("IM_DATA_DIR"), dataFile.getName());

        // set the user config
        ValidationResponse validationResponse = DataFileManager.processDataFile(dataFile, fileLocation);
        if (validationResponse.isValid) {

            DataFileProperties dataFileProperties = validationResponse.dataFileProperties;

            // add answers
            List<DataFilePropertiesResponseAnswers> answers = body.getAnswers();
            mineConfigManager.setSelectedAnswers(repository, mineId, dataFileProperties, answers);

            mineConfigManager.addFileProperties(repository, mineId, fileId, dataFileProperties);
            return new ResponseEntity(HttpStatus.OK);
        }
        throw new IllegalArgumentException(validationResponse.errorMessage);
    }


}