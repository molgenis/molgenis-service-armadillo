<template>
  <footer class="text-primary text-center py-3 border-top">
    <div class="container">
      <p class="mb-0">
        <small class="text-muted"
          >Please cite
          <a href="https://doi.org/10.1093/bioinformatics/btae726">
            Cadman et al. (2024)
          </a>
          when publishing research conducted using Armadillo
          <a
            href="https://www.ravelry.com/patterns/library/roll-up-armadillo"
            target="_blank"
          >
            <i class="bi bi-egg"></i>
          </a>
          .
        </small>
      </p>
      <p class="mb-0">
        <small class="text-muted">
          This platform was created using
          <a href="https://molgenis.org/">MOLGENIS.org</a>
          <a href="https://github.com/molgenis/molgenis-service-armadillo">
            (Github)
          </a>
          .
        </small>
      </p>
    </div>
  </footer>
</template>

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "Footer",
});
</script>
