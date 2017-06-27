package org.librairy.rhetoric.service;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import org.librairy.boot.model.domain.resources.*;
import org.librairy.boot.storage.UDM;
import org.librairy.boot.storage.dao.AnnotationsDao;
import org.librairy.boot.storage.dao.DomainsDao;
import org.librairy.boot.storage.dao.ItemsDao;
import org.librairy.boot.storage.generator.URIGenerator;
import org.librairy.rhetoric.parser.RhetoricalParser;
import org.librairy.rhetoric.utils.Worker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class RhetoricalService {

    private static final Logger LOG = LoggerFactory.getLogger(RhetoricalService.class);

    @Autowired
    UDM udm;

    @Autowired
    AnnotationsDao annotationsDao;

    @Autowired
    RhetoricalParser parser;

    @Autowired
    Worker worker;

    @Autowired
    DomainsDao domainsDao;

    @Autowired
    ItemsDao itemsDao;

    public void handleParallel(String itemUri){
        worker.run(() -> handle(itemUri));
    }

    public void handle(String itemUri){

        try{
            Optional<Resource> optResource = udm.read(Resource.Type.ITEM).byUri(itemUri);

            if (!optResource.isPresent()){
                LOG.warn("No ITEM found by uri:  " + itemUri);
                return;
            }

            Item item = optResource.get().asItem();

            LOG.info("Parsing '" + item.getUri() + "' ...");

            String content = item.getContent();

            if (Strings.isNullOrEmpty(content)){
                LOG.info("No content found by uri: '" + itemUri + "'");
                return;
            }


            Instant start = Instant.now();

            // Get rhetorical parts
            String itemId = URIGenerator.retrieveId(item.getUri());
            parser.getParts(itemId, content).entrySet().forEach(rhetoricalPart -> {

                if (Strings.isNullOrEmpty(rhetoricalPart.getValue())){
                    LOG.warn("Rhetorical part '" + rhetoricalPart.getKey() + "' is empty for item: " + itemUri);
                }else{
                    LOG.debug("Creating a new part from rhetoric '" + rhetoricalPart.getKey() + "' ...");
                    // TODO annotation when digitalObject
                    Part part = new Part();
                    part.setSense(rhetoricalPart.getKey());
                    part.setContent(rhetoricalPart.getValue());
                    udm.save(part);

                    itemsDao.addPart(itemUri, part.getUri());
                    LOG.info("Rhetorical part '" + rhetoricalPart.getKey() + "' created for item: " + itemUri);
                }

            });

            Instant end = Instant.now();
            LOG.info("Annotated '" + itemUri + "'  in: " + ChronoUnit.MINUTES.between(start,end) + "min " + (ChronoUnit.SECONDS.between(start,end)%60) + "secs");

        }catch (Exception e){
            LOG.warn("Unexpected error",e);
        }
    }

}

