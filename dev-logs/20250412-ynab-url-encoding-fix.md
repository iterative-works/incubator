# YNAB API URL Encoding Fix

Date: 2025-04-12

## Problem

The `YnabClient` class had a potential security issue with URL encoding. The code was using string interpolation to build API endpoint URLs:

```scala
get(buildUri(s"/budgets/$budgetId/accounts"))
```

When variables (like `budgetId`) contain special characters that need URL encoding, string interpolation doesn't handle this properly. This could lead to malformed URLs or potential security issues.

## Solution

Refactored the `buildUri` method to accept path segments as variable arguments and properly encode each segment:

```scala
private def buildUri(segments: String*): Uri =
    val baseUri = Uri.parse(baseUrl).getOrElse(throw new IllegalArgumentException(s"Invalid base URL: $baseUrl"))
    // Add each segment to the base URI, ensuring proper URL encoding
    baseUri.addPath(segments.toList)
```

Updated all API endpoint calls to use this new approach, for example:

```scala
// Before
get(buildUri(s"/budgets/$budgetId/accounts"))

// After
get(buildUri("budgets", budgetId, "accounts"))
```

This ensures that dynamic path segments like `budgetId` are properly URL-encoded regardless of what characters they contain.

## Implementation Details

1. Changed the `buildUri` method signature to accept variable arguments (varargs)
2. Used sttp's `addPath` method with a list of segments for proper encoding
3. Updated all API endpoint calls throughout the class
4. Removed leading slashes from path segments for consistency

## Benefits

- Improved security by properly encoding URL parameters
- Reduced risk of URL injection vulnerabilities
- More robust handling of special characters in identifiers
- Better type safety for URL construction

## Testing

Verified changes by running diagnostics to ensure no errors or warnings were introduced.