package org.molgenis.armadillo.minio;

import static com.google.common.collect.Multimaps.flatteningToMultimap;
import static java.util.function.Function.identity;

import com.google.common.collect.Multimap;
import com.google.common.collect.MultimapBuilder;
import io.minio.messages.Bucket;
import io.minio.messages.Item;
import java.io.InputStream;
import java.util.stream.Stream;
import org.molgenis.armadillo.service.StorageService;
import org.springframework.stereotype.Service;

@Service
public class TableService {

  public static final String SHARED_PREFIX = "shared-";
  private final StorageService storageService;

  public TableService(StorageService storageService) {
    this.storageService = storageService;
  }

  public Multimap<String, String> listTables() {
    return storageService.listBuckets().stream()
        .map(Bucket::name)
        .filter(it -> it.startsWith(SHARED_PREFIX))
        .collect(
            flatteningToMultimap(
                identity(),
                this::getTablesInBucket,
                MultimapBuilder.treeKeys().treeSetValues()::build));
  }

  Stream<String> getTablesInBucket(String bucketName) {
    return storageService.listObjects(bucketName).stream().map(Item::objectName);
  }

  public InputStream loadTable(String table) {
    int index = table.indexOf('/');
    String folder = table.substring(0, index);
    String objectName = table.substring(index + 1);
    return storageService.load(SHARED_PREFIX + folder, objectName);
  }
}
