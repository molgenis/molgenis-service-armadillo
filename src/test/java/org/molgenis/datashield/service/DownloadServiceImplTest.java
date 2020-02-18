package org.molgenis.datashield.service;

import net.bytebuddy.implementation.bind.MethodDelegationBinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.api.metadata.model.*;
import org.molgenis.datashield.service.model.Table;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class DownloadServiceImplTest {

  private DownloadService downloadService;

  @Mock private RestTemplate restTemplate;

//  @BeforeEach
//  void before() {
//
//    downloadService = new DownloadServiceImpl(restTemplate);
//    Mockito.when(restTemplate.getForObject(DownloadServiceImpl.METADATA_URL, EntityType.class, "aaabbbcccddd")).thenReturn(entityType);
//  }

  @Test
  void getMetadata() {
//    Table entityMetaData = downloadService.getMetadata("aaabbbcccddd");
//    System.out.println(entityMetaData.name());
  }

  @Test
  void download() {}
}
