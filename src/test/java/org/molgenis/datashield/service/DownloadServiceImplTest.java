package org.molgenis.datashield.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.molgenis.api.metadata.model.EntityType;
import org.molgenis.datashield.service.model.Column;
import org.molgenis.datashield.service.model.ColumnType;
import org.molgenis.datashield.service.model.Table;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DownloadServiceImplTest {

  private DownloadService downloadService;

  @Mock private RestTemplate restTemplate;

  @BeforeEach
  void before() {
    downloadService = new DownloadServiceImpl(restTemplate);
  }

  @Test
  void testGetMetadata() {
    EntityType patients = TestUtils.getEntityType("project_patients.json");
    EntityType visits = TestUtils.getEntityType("project_visits.json");
    when(restTemplate.getForObject(
            DownloadServiceImpl.METADATA_URL, EntityType.class, "project_patients"))
        .thenReturn(patients);
    when(restTemplate.getForObject(
            "http://localhost/api/metadata/project_visits?flattenAttributes=true",
            EntityType.class))
        .thenReturn(visits);

    Table entityMetaData = downloadService.getMetadata("project.patients");

    assertEquals("project.patients", entityMetaData.name());
  }

  @Test
  void testDownload() {
    Column id = Column.builder().setName("column_id").setType(ColumnType.INT).build();
    Table table = Table.builder().setName("table_patients").addColumn(id).build();
    when(restTemplate.postForEntity(
            eq(DownloadServiceImpl.DOWNLOAD_URL), any(HttpEntity.class), eq(Resource.class)))
        .thenReturn(ResponseEntity.ok().build());

    assertDoesNotThrow(() -> downloadService.download(table));
  }
}
