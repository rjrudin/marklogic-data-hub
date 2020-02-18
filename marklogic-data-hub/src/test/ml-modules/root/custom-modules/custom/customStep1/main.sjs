const DataHub = require("/data-hub/5/datahub.sjs");
const datahub = new DataHub();

function main(content, options) {
  let id = content.uri;
  if (options.failForUris && options.failForUris.indexOf(id) > -1) {
    throw new Error("Intentionally throwing error for URI: " + id);
  }

  let doc = content.value;
  if (doc && (doc instanceof Document || doc instanceof XMLDocument)) {
    doc = fn.head(doc.root);
  }

  let instance = datahub.flow.flowUtils.getInstance(doc);
  let envelope = datahub.flow.flowUtils.makeEnvelope(instance, {}, [], "json");

  content.value = envelope;
  return content;
}

module.exports = {
  main: main
};
