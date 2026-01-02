# Build the project
build:
    ./gradlew build

# Clean and build the project
rebuild:
    ./gradlew clean build

# Run the application
run:
    ./gradlew bootRun

# Check code styling
check-style:
    ./gradlew spotlessCheck

# Apply code styling
format:
    ./gradlew spotlessApply

# Java linting
lint:
    ./gradlew checkstyleMain

# Admin UI commands
admin-install:
    cd src/main/resources/static/admin && npm install

admin-dev:
    cd src/main/resources/static/admin && npm run dev

admin-build:
    cd src/main/resources/static/admin && npm run build

admin-typecheck:
    cd src/main/resources/static/admin && npm run typecheck

admin-lint:
    cd src/main/resources/static/admin && npm run lint

admin-check:
    cd src/main/resources/static/admin && npm run check

admin-fix:
    cd src/main/resources/static/admin && npm run check:fix

# Build Docker image
docker-build:
    docker build -t kaillux .

# Run application in Docker
docker-run:
    docker run -p 8080:8080 -p 27888:27888/udp -p 27889-27924:27889-27924/udp kaillux
