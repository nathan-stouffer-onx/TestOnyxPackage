LAST_VERSION=$(git tag -l --sort=-version:refname | head -n 1)
NO_PATCH=$(echo ${LAST_VERSION} | cut -f1-2 -d.)
if [ "${NO_PATCH}" = "${MAJOR_VERSION}.${MINOR_VERSION}" ]; then
    LAST_PATCH=$(echo ${LAST_VERSION} | cut -f3 -d.)
    PATCH=$((${LAST_PATCH} + 1))
else
    PATCH=0
fi
VERSION=${MAJOR_VERSION}.${MINOR_VERSION}.${PATCH}
echo ${VERSION}
