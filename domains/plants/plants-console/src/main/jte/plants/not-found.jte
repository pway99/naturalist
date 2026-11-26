@param String name
@param String back = "/plants/orders"

@template.layout.page(title = "Not in the catalog", content = @`
    <h1>Not in the catalog</h1>
    <p>No plant record named <code>${name}</code> is in the catalog.</p>
    <p><a href="${back}">Back to the list</a></p>
`)
