#this scripts takes arguments
NAME=$1

cp /ci/ci.env /scripts/release/.env
echo "Using name $NAME"

sed -i "s|^ARMADILLO_URL=.*|ARMADILLO_URL=${NAME}.dev.molgenis.org|" /scripts/release/.env
sed -i "s|^ARMADILLO_PASSWORD=.*|ARMADILLO_PASSWORD=${ADMINPASS}|" /scripts/release/.env

