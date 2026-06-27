# ==============================================================================
# The broad operation registry for the throughput survey (bench.R), the call-form
# validation (probe.R), and the primitive extraction (capture.R). Source AFTER
# bench_lib.R (which loads config.R + the DS packages).
#
# Nested  category -> op-name -> function(cn).  The category travels with each op
# (so it reaches the output CSV and the plot needs no separate map), and every op
# takes the LIVE connection `cn` as an argument (resolved per cell), so a
# reconnect after a crash is actually used. Call forms are taken from each
# function's dsBaseClient smoke test (tests/testthat/test-smk-ds.*). Server
# symbols: D = CNSIM, D2 = CNSIM_B (merge), DS = survival, DC = cluster.
# Add a function by adding one line in the right category (+ a PREP_FOR entry if
# it needs a prerequisite object).
# ==============================================================================

suppressMessages(library(dsBaseClient))

ds_ops <- function(be) list(
  io = list(
    datashield.assign.table = function(cn) datashield.assign.table(cn, "scratch", table_a_ref(be))
  ),
  descriptive = list(
    ds.mean         = function(cn) ds.mean(x = "D$LAB_TSC", type = "combine", datasources = cn),
    ds.var          = function(cn) ds.var(x = "D$LAB_TSC", type = "combine", datasources = cn),
    ds.quantileMean = function(cn) ds.quantileMean(x = "D$LAB_HDL", datasources = cn),
    ds.summary      = function(cn) ds.summary(x = "D$LAB_TSC", datasources = cn)
  ),
  correlation = list(
    ds.cor = function(cn) ds.cor(x = "D$LAB_TSC", y = "D$LAB_HDL", type = "combine", datasources = cn)
  ),
  metadata = list(
    ds.dim      = function(cn) ds.dim(x = "D", type = "combine", datasources = cn),
    ds.length   = function(cn) ds.length(x = "D$LAB_TSC", type = "combine", datasources = cn),
    ds.colnames = function(cn) ds.colnames(x = "D", datasources = cn),
    ds.class    = function(cn) ds.class(x = "D$LAB_TSC", datasources = cn),
    ds.exists   = function(cn) ds.exists(x = "D", datasources = cn),
    ds.numNA    = function(cn) ds.numNA(x = "D$LAB_HDL", datasources = cn),
    ds.levels   = function(cn) ds.levels(x = "D$GENDER", datasources = cn),
    ds.ls       = function(cn) ds.ls(datasources = cn)
  ),
  coercion = list(
    ds.asInteger    = function(cn) ds.asInteger(x.name = "D$GENDER", newobj = "ai", datasources = cn),
    ds.asCharacter  = function(cn) ds.asCharacter(x.name = "D$GENDER", newobj = "ac", datasources = cn),
    ds.asFactor     = function(cn) ds.asFactor(input.var.name = "D$DIS_CVA", newobj.name = "af", datasources = cn),
    ds.asDataMatrix = function(cn) ds.asDataMatrix(x.name = "D$GENDER", newobj = "adm", datasources = cn)
  ),
  transform = list(
    ds.assign = function(cn) ds.assign(toAssign = "D$LAB_TSC*2", newobj = "ao", datasources = cn),
    ds.make   = function(cn) ds.make(toAssign = "D$LAB_TSC*2", newobj = "md", datasources = cn),
    ds.Boole  = function(cn) ds.Boole(V1 = "D$LAB_TSC", V2 = "D$LAB_TRIG", Boolean.operator = "==", newobj = "bo", datasources = cn)
  ),
  recode = list(
    ds.recodeValues   = function(cn) ds.recodeValues(var.name = "D$DIS_CVA", values2replace.vector = c(0, 1), new.values.vector = c(10, 20), newobj = "rv", datasources = cn),
    ds.recodeLevels   = function(cn) ds.recodeLevels(x = "D$GENDER", newCategories = c("g0", "g1"), newobj = "rl", datasources = cn),
    ds.changeRefGroup = function(cn) ds.changeRefGroup(x = "D$GENDER", ref = "1", newobj = "crg", datasources = cn)
  ),
  vector = list(
    ds.rep       = function(cn) ds.rep(x1 = 4, times = 6, length.out = NA, each = 1, source.x1 = "clientside", source.times = "c", source.length.out = NULL, source.each = "c", x1.includes.characters = FALSE, newobj = "rep1", datasources = cn),
    ds.replaceNA = function(cn) ds.replaceNA(x = "D$LAB_HDL", forNA = list(0), newobj = "rna", datasources = cn)
  ),
  dataframe = list(
    ds.dataFrameSubset = function(cn) ds.dataFrameSubset(df.name = "D", V1.name = "D$LAB_TSC", V2.name = "D$LAB_HDL", Boolean.operator = "!=", newobj = "sub_row", datasources = cn),
    ds.dataFrame       = function(cn) ds.dataFrame(x = c("D$LAB_TSC", "D$LAB_HDL"), newobj = "df2", datasources = cn),
    ds.cbind           = function(cn) ds.cbind(x = c("D$LAB_TSC", "D$LAB_HDL"), newobj = "cb", datasources = cn),
    ds.merge           = function(cn) ds.merge(x.name = "D", y.name = "D2", by.x.names = "key", by.y.names = "key", newobj = "mg", datasources = cn)
  ),
  reshape = list(
    ds.reShape = function(cn) ds.reShape(data.name = "DS", v.names = "age.60", timevar.name = "time.id", idvar.name = "id", direction = "wide", newobj = "rsh", datasources = cn)
  ),
  tabulation = list(
    ds.table = function(cn) ds.table(rvar = "D$GENDER", cvar = "D$DIS_CVA", datasources = cn)
  ),
  glm = list(
    ds.glm     = function(cn) ds.glm(formula = "LAB_TSC ~ LAB_TRIG", data = "D", family = "gaussian", datasources = cn),
    ds.glmSLMA = function(cn) ds.glmSLMA(formula = "LAB_TSC ~ LAB_TRIG", family = "gaussian", dataName = "D", newobj = "glmslma", datasources = cn)
  ),
  `mixed-model` = list(
    ds.lmerSLMA = function(cn) ds.lmerSLMA(formula = "incid_rate ~ trtGrp + Male + (1|idDoctor)", dataName = "DC", datasources = cn)
  ),
  objects = list(
    ds.rm = function(cn) { ds.assign(toAssign = "D$LAB_TSC", newobj = "torm", datasources = cn); ds.rm(x.names = "torm", datasources = cn) }
  ),
  # DSI infrastructure (datashield.*) that hits the server. login/logout/
  # workspace_load are timed separately by session_rows() in bench.R.
  dsi = list(
    datashield.tables         = function(cn) datashield.tables(cn),
    datashield.pkg_status     = function(cn) datashield.pkg_status(cn),
    datashield.profiles       = function(cn) datashield.profiles(cn),
    datashield.workspaces     = function(cn) datashield.workspaces(cn),
    datashield.aggregate      = function(cn) datashield.aggregate(cn, "dimDS('D')"),
    datashield.assign.expr    = function(cn) datashield.assign.expr(cn, "ae2", "D$LAB_TSC * 2"),
    datashield.workspace_save = function(cn) datashield.workspace_save(cn, "benchws")
  )
)

# Flatten the nested registry to a list of {op, category, fn} for one backend.
flatten_ops <- function(be) {
  nested <- ds_ops(be)
  out <- list()
  for (cat in names(nested))
    for (op in names(nested[[cat]]))
      out[[length(out) + 1]] <- list(op = op, category = cat, fn = nested[[cat]][[op]])
  out
}

# Prerequisite builders: run untimed (after reset) for ops that need a prior
# server object. Keyed by op name; ops not listed need no prep. The current core
# function set has no such ops, so this is empty (kept so callers can look up).
PREP_FOR <- list()
