# Integration review

- The four `agy` work streams produced the Java foundation and SQL schema, recommendation module, catalog/saved module, and React client in separate paths.
- A catalog/saved route CORS failure was found during an HTTP smoke test and fixed with explicit local Vite origins. The API now serves the catalog and saved courses through both direct HTTP and the Vite proxy.
- Career scoring was giving unrelated engineering courses points from the word "Engineer" alone. Partial career matching now uses subject tokens, and a regression test covers this.
- The generic exception handler was returning exception messages that could contain SQL or internal details. It now logs the exception server side and returns a generic error to clients.
- Demo seeding now checks each course title before insertion, so a partially seeded catalog can be completed without duplicating existing demo courses.
- Final backend package and all 27 tests passed. The frontend production build passed. The HTTP smoke flow and a browser flow through registration, profile editing, catalog search, recommendations, and saving a course passed. The recommendation session's final network response failed, but its output was present and independently verified.

The sample course catalog is illustrative. Provider links and current availability are not verified. The recommendation score is a weighted heuristic, not a trained model.
