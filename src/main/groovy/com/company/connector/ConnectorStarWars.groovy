package com.company.connector

import groovy.util.logging.Slf4j
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.bonitasoft.engine.connector.AbstractConnector
import org.bonitasoft.engine.connector.ConnectorException
import org.bonitasoft.engine.connector.ConnectorValidationException
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory

@Slf4j
class ConnectorStarWars extends AbstractConnector {

    def static final NAME_INPUT = "name"
    def static final URL_INPUT = "url"
    def static final PERSON_OUTPUT = "person"

    def StarWarsService service

    @Override
    def void validateInputParameters() throws ConnectorValidationException {
        checkMandatoryStringInput(NAME_INPUT)
        checkMandatoryStringInput(URL_INPUT)
    }

    def checkMandatoryStringInput(inputName) throws ConnectorValidationException {
        def value = getInputParameter(inputName)
        if (value in String) {
            if (!value) {
                throw new ConnectorValidationException(this, "Mandatory parameter '$inputName' is missing.")
            }
        } else {
            throw new ConnectorValidationException(this, "'$inputName' parameter must be a String")
        }
    }

    @Override
    def void executeBusinessLogic() throws ConnectorException {
        def name = getInputParameter(NAME_INPUT)
        log.info "$NAME_INPUT : $name"
        // Retrieve the retrofit service created during the connect phase, call the 'person' endpoint with the name parameter
        def response = getService().person(name).execute()
        if (response.isSuccessful()) {
            def persons = response.body.getPersons()
            if (!persons.isEmpty()) {
                def person = persons[0]
                setOutputParameter(PERSON_OUTPUT, person)
            } else {
                throw new ConnectorException("$name not found")
            }
        } else {
            throw new ConnectorException(response.message())
        }
    }

    @Override
    def void connect() throws ConnectorException {
        def httpClient = createHttpClient(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
        service = createService(httpClient, getInputParameter(URL_INPUT))
    }

    def static OkHttpClient createHttpClient(Interceptor... interceptors) {
        def clientBuilder = new OkHttpClient.Builder()
        if (interceptors) {
            interceptors.each { clientBuilder.interceptors().add(it) }
        }
        clientBuilder.build()
    }

    def static StarWarsService createService(OkHttpClient client, String baseUrl) {
        new Retrofit.Builder()
                .client(client)
                .addConverterFactory(JacksonConverterFactory.create())
                .baseUrl(baseUrl)
                .build()
                .create(StarWarsService.class)
    }

    /**
     * [Optional] Close connection to remote server
     */
    @Override
    void disconnect() throws ConnectorException{}
}
