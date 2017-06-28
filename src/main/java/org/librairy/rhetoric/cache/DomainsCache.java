package org.librairy.rhetoric.cache;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.librairy.boot.model.domain.resources.Domain;
import org.librairy.boot.storage.dao.ItemsDao;
import org.librairy.boot.storage.generator.URIGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class DomainsCache {

    private static final Logger LOG = LoggerFactory.getLogger(DomainsCache.class);

    private LoadingCache<String, List<Domain>> cache;

    @Autowired
    ItemsDao itemsDao;

    @PostConstruct
    public void setup(){
        this.cache = CacheBuilder.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .build(
                        new CacheLoader<String, List<Domain>>() {
                            public List<Domain> load(String itemUri) {

                                List<Domain> domains = new ArrayList<Domain>();
                                Integer size = 200;
                                Optional<String> offset = Optional.empty();

                                while(true){
                                    List<Domain> partialDomains = itemsDao.listDomains(itemUri, size, offset, false);

                                    domains.addAll(partialDomains);

                                    if (partialDomains.size() < size) break;

                                    offset = Optional.of(URIGenerator.retrieveId(partialDomains.get(size-1).getUri()));

                                }

                                return domains;
                            }
                        });
    }


    public List<Domain> getDomainsFrom(String itemUri)  {
        try {
            return this.cache.get(itemUri);
        } catch (ExecutionException e) {
            LOG.error("error getting domains from database", e);
            return Collections.emptyList();
        }
    }

}
