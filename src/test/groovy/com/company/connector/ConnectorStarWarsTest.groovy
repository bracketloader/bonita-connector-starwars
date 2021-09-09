
package com.company.connector

import com.company.connector.model.Person
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.bonitasoft.engine.connector.ConnectorValidationException
import org.bonitasoft.engine.connector.ConnectorException
import spock.lang.Specification

import static com.company.connector.StarWarsService.*

class ConnectorStarWarsTest extends Specification {

    def server
    def connector

    def setup() {
        server = new MockWebServer()
        def url = server.url("/")
        def baseUrl = "http://${url.host}:${url.port}"

        def httpClient = ConnectorStarWars.createHttpClient(new HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        def service = ConnectorStarWars.createService(httpClient, baseUrl)

        connector = new ConnectorStarWars()
        connector.service = service
    }

    def cleanup() {
        server.shutdown();
    }

    def should_throw_exception_if_mandatory_input_is_missing() {
        given: 'Connector with missing input'
        def connector = new ConnectorStarWars()

        when: 'Validating inputs'
        connector.validateInputParameters()

        then: 'ConnectorValidationException is thrown'
        thrown ConnectorValidationException
    }

    def should_throw_exception_if_mandatory_input_is_empty() {
        given: 'A connector without an empty input'
        def connector = new ConnectorStarWars()
        connector.setInputParameters([(ConnectorStarWars.NAME_INPUT):''])

        when: 'Validating inputs'
        connector.validateInputParameters()

        then: 'ConnectorValidationException is thrown'
        thrown ConnectorValidationException
    }

    def should_throw_exception_if_mandatory_input_is_not_a_string() {
        given: 'A connector without an integer input'
        def connector = new ConnectorStarWars()
        connector.setInputParameters([(ConnectorStarWars.NAME_INPUT):38])

        when: 'Validating inputs'
        connector.validateInputParameters()

        then: 'ConnectorValidationException is thrown'
        thrown ConnectorValidationException
    }

    /**
    * Connector unit test - no internet required
    */
    def should_fetch_person() {
        given: 'A person name'
        def name = 'Luke'
        and: 'A related person JSON response'
        def body = """
            {"results": [
                {
                    "name":"$name Skywalker",
                    "height":"172",
                    "mass":"77",
                    "hair_color":"blond",
                    "skin_color":"fair",
                    "eye_color":"blue",
                    "birth_year":"19BBY",
                    "gender":"male",
                    "homeworld":"http://swapi.dev/api/planets/1/"
                }
            ]}
        """
        server.enqueue(new MockResponse().setBody(body))

        when: 'Executing connector'
        connector.setInputParameters(['name': name])
        connector.executeBusinessLogic()

        then: 'Connector output should contain the person data'
        def outputParameters = connector.outputParameters
        outputParameters.size() == 1

        def person = outputParameters.get(ConnectorStarWars.PERSON_OUTPUT)
        person instanceof Person
        person.name == "Luke Skywalker"
    }

    /**
    * Connector unit test - no internet required
    */
    def should_get_unknown_person() {
        given: 'An API server'
        String body = "{\"results\":[]}"
        server.enqueue(new MockResponse().setBody(body))

        when: 'Executing business logic'
        def name = 'Luke'
        connector.setInputParameters(['name': name])
        connector.executeBusinessLogic()

        then: 'Connector should throw exception'
        def e = thrown(ConnectorException)
        e.getMessage() == "$name not found"
    }

    /**
    * Connector unit test - no internet required
    */
    def should_handle_server_error() {
        given: 'An API server'
        server.enqueue(new MockResponse().setResponseCode(500))

        when: 'Executing business logic'
        def name = 'Luke'
        connector.setInputParameters(['name': name])
        connector.executeBusinessLogic()

        then: 'Connector should throw exception'
        def e = thrown(ConnectorException)
        e.getMessage() == "Server Error"
    }
}