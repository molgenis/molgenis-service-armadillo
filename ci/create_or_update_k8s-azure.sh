#this scripts takes arguments
NAME=$1
TAG_NAME=$2
DELETE=$3

echo "Using namespace $NAME"
echo "Using docker tag_name $TAG_NAME"
echo "Delete=$DELETE"

REPO=molgenis/molgenis-armadillo
if [[ $TAG_NAME == *"SNAPSHOT"* ]]; then
  REPO=molgenis/molgenis-armadillo-snapshot
fi
echo "Using repositorie $REPO" 

# delete if exists
if [ ! -z "$DELETE" ]
then
  kubectl delete namespace $NAME || true
fi
# Create certs from environement
echo ${CERTDEVMOLGENIS_KEY} | base64 --decode >> /tmp/cert_key 
echo ${CERTDEVMOLGENIS_PEM} | base64 --decode >> /tmp/cert_pem

# wait for deletion to complete
sleep 15s
kubectl create namespace $NAME
kubectl create secret tls "dev.molgenis.org" --key /tmp/cert_key --cert /tmp/cert_pem -n armadillo-${NAME}

helm upgrade --install armadillo-${NAME} ./helm-chart --namespace armadillo-${NAME} \
--set ingress.hosts[0].paths[0].path=/ \
--set ingress.hosts[0].paths[0].pathType=ImplementationSpecific \
--set ingress.hosts[0].host=armadillo-${NAME}.dev.molgenis.org \
--set ingress.tls[0].host=armadillo-${NAME}.dev.molgenis.org \
--set ingress.tls[0].secretName=dev.molgenis.org \
--set adminPassword=adminArmadillo! \
--set image.tag=${TAG_NAME} \
--set image.pullPolicy=Always \

rm /tmp/cert_key
rm /tmp/cert_pem

