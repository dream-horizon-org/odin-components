# Component Schema Guiding Principles

## Table of Contents
1. [Core Architecture: Definition vs Provisioning](#core-architecture-definition-vs-provisioning)
2. [The Liskov Substitution Principle (LSP)](#the-liskov-substitution-principle-lsp)
3. [Property Placement Decision Framework](#property-placement-decision-framework)
4. [Default Values Philosophy](#default-values-philosophy)
5. [Property Description Guidelines](#property-description-guidelines)
6. [API Mapping and Validation](#api-mapping-and-validation)
7. [Common Pitfalls and Lessons Learned](#common-pitfalls-and-lessons-learned)
8. [Documentation and README Generation](#documentation-and-readme-generation)

---

## Core Architecture: Definition vs Provisioning

### The Fundamental Separation

Every Odin component follows a strict separation of concerns:

```
Component/
├── schema.json          # WHAT: Component Definition
├── defaults.json        # Default values for definition
└── <flavour>/
    ├── schema.json      # HOW/WHERE: Provisioning specifics
    └── defaults.json    # Default values for provisioning
```

### Component Definition (Root Level)
**Purpose:** Defines WHAT the software is - its fundamental, logical properties

**Characteristics:**
- Properties that fundamentally change how clients interact with the component
- Settings that require different client libraries or connection logic
- Configuration that defines the core behavior of the software
- Properties that must be consistent across ALL flavours

**Examples:**
```json
{
  "redisVersion": "7.1",           // Determines client protocol compatibility
  "clusterModeEnabled": true,      // Changes connection logic fundamentally
  "authentication": {              // Affects how clients authenticate
    "enabled": true,
    "authToken": "secret"
  }
}
```

### Provisioning (Flavour Level)
**Purpose:** Defines HOW and WHERE the component runs

**Characteristics:**
- Platform-specific implementation details
- Operational tuning parameters
- Infrastructure configuration
- Settings that don't change the client interaction model

**Examples:**
```json
{
  "cacheNodeType": "cache.t4g.micro",     // AWS-specific instance type
  "securityGroupIds": ["sg-123"],         // AWS networking
  "snapshotRetentionLimit": 7,            // Operational backup config
  "multiAzEnabled": true                  // AWS availability config
}
```

---

## The Liskov Substitution Principle (LSP)

### Definition
**Any property in the root definition MUST be implementable across ALL flavours without breaking the contract.**

### Why LSP Matters

Properties in the root definition create a contract that ALL flavours must fulfill. If a flavour cannot support a root property, it violates LSP and breaks the component's abstraction.

### The Critical Test

Before adding a property to root definition, ask:
1. Can **every** flavour support this property?
2. Does the property work the same way across all platforms?
3. Are there hidden dependencies that might prevent configuration?

### Real-World LSP Violations

#### Example 1: Port Configuration
```json
// WRONG - In root definition
{
  "port": 6379  // ❌ ElastiCache fixes this to 6379, not configurable
}
```
**Why it fails:** Managed services like AWS ElastiCache don't allow port configuration

#### Example 2: Persistence Mode
```json
// WRONG - In root definition
{
  "persistenceMode": "aof"  // ❌ ElastiCache manages this internally
}
```
**Why it fails:** Cloud providers abstract away persistence configuration

#### Example 3: The Subtle Case - notifyKeyspaceEvents
```json
// APPEARS CORRECT but FAILS LSP
{
  "notifyKeyspaceEvents": "Ex"  // ❌ Requires custom parameter group in AWS
}
```
**Why it fails:**
- In AWS ElastiCache, this requires a custom parameter group
- Default parameter groups cannot be modified
- If user doesn't provide `parameterGroupName`, this property cannot be set
- Creates a hidden dependency, violating LSP

### LSP Validation Checklist

✅ **Property passes LSP if:**
- It can be configured in ALL flavours
- It works identically across platforms
- It has no hidden dependencies

❌ **Property fails LSP if:**
- Any flavour cannot support it
- It requires platform-specific workarounds
- It depends on other optional configurations
- Managed services abstract it away

---

## Property Placement Decision Framework

### The Decision Tree

```
┌─────────────────┐
│  New Property   │
└────────┬────────┘
         │
         ▼
┌──────────────────────┐     NO       ┌─────────────────────┐
│ Changes client       │─────────────►│ Provisioning/Flavour│
│ interaction model?   │              └─────────────────────┘
└──────────┬───────────┘                        ▲
           │ YES                                │
           ▼                                    │
┌──────────────────────┐     NO                 │
│ Is property truly    │────────────────────────┘
│ flavour-agnostic?    │
└──────────┬───────────┘                        ▲
           │ YES                                │
           ▼                                    │
┌──────────────────────┐     NO                 │
│ Supported by ALL     │────────────────────────┘
│ flavours?            │
└──────────┬───────────┘                        ▲
           │ YES                                │
           ▼                                    │
┌──────────────────────┐     NO                 │
│ No hidden            │────────────────────────┘
│ dependencies?        │
└──────────┬───────────┘
           │ YES
           ▼
┌──────────────────────┐
│   Root Definition    │
└──────────────────────┘
```

### Property Placement Examples

| Property | Location | Reasoning |
|----------|----------|-----------|
| `redisVersion` | Root | All flavours must support version selection |
| `clusterModeEnabled` | Root | Fundamentally changes client connection model |
| `cacheNodeType` | AWS Flavour | AWS-specific instance types |
| `port` | Self-managed Flavour only | Managed services fix this value |
| `notifyKeyspaceEvents` | Flavour | Requires platform-specific setup (parameter groups) |
| `securityGroupIds` | AWS Flavour | AWS-specific networking |



## Default Values Philosophy

### Core Principle
**Defaults should enable quick starts with minimal configuration while guiding users toward production readiness through documentation.**

### Default Strategy

#### Development-Friendly Defaults
```json
{
  "replicasPerNodeGroup": 0,        // Minimize cost for dev
  "transitEncryptionEnabled": false, // Simplify client setup
  "snapshotRetentionLimit": 0,      // No backup costs
  "automaticFailoverEnabled": false // Simple single-node setup
}
```

#### But Security-First Where Critical
```json
{
  "autoMinorVersionUpgrade": true    // Security patches by default
}
```

### When to Provide Defaults

#### MUST Have Default When:
1. **Common Starting Point:** Most users want the same initial value
2. **Opt-in Features:** Safe to disable by default
3. **Best Practices:** Enables recommended settings
4. **Developer Experience:** Reduces initial configuration burden

#### MUST NOT Have Default When:
1. **Environment-Specific:** VPCs, subnets, security groups
2. **Secrets:** Passwords, tokens, keys
3. **Fundamental Choices:** Component identity (IDs, versions)
4. **Cost-Impacting:** Decisions with significant budget implications

### Smart Defaults Pattern

Transform API-required fields into optional fields through defaults:

```json
// AWS requires replicationGroupDescription
// But we provide a default to make it optional for users
{
  "replicationGroupDescription": {
    "type": "string",
    "default": "ElastiCache Redis replication group",
    "description": "Human-readable description..."
  }
}
```

---

## Property Description Guidelines

### Every description should explain:
1. **WHAT** - What is this property?
2. **WHY** - Why would you change it?
3. **IMPACT** - What happens when you change it?

### Description Template

```json
{
  "propertyName": {
    "description": "[WHAT it is]. [WHY you'd set it]. [IMPACT/TRADEOFFS]. **Default: `value`** ([why this default]). **Production:** [specific recommendation]."
  }
}
```

### Real Examples

#### Excellent Description
```json
{
  "transitEncryptionEnabled": {
    "description": "Enables TLS to encrypt data in transit between clients and the Redis server. Enable for production workloads handling sensitive data. Note: may impact performance by 10-20%. **Default: `false`** (disabled to simplify client configuration for development/testing). **Production:** Enable for sensitive data or compliance requirements."
  }
}
```

#### Poor Description
```json
{
  "transitEncryptionEnabled": {
    "description": "Enable transit encryption"  // ❌ No context, impact, or guidance
  }
}
```

### Production Guidance Requirements

Always include:
- Specific recommendations for production use
- Performance implications (latency, throughput impact)
- Cost implications (resource multiplication, storage costs)
- Security considerations
- Scaling guidance

---

## Cloud Provider API Mapping and Validation
This section is applicable for flavours that encapsulates cloud vendor offerings e.g. Redis - AWS Elasticache.
### Cloud Provider API Verification Requirements

Before adding ANY property to a flavour schema:

1. **Keep The Exact API Parameter Name But In camelCase**
   ```
   Cloud Vendor's API Parameter: CacheParameterGroupName
   Schema Property: parameterGroupName ❌, should be cacheParameterGroupName
   ```

2. **Follow Same Validate Constraints As of Cloud Vendor**
   ```json
   {
     "replicationGroupId": {
       "pattern": "^[a-z][a-z0-9\\-]{0,39}$",  // Must match API requirements
       "maxLength": 40
     }
   }
   ```
---

## Common Pitfalls and Lessons Learned

### 1. The Hidden Dependency Trap

**Problem:** Property seems universal but requires platform-specific setup
```json
// notifyKeyspaceEvents of Redis requires custom parameter groups in AWS
// Default parameter groups cannot be modified!
```

**Lesson:** Always test with DEFAULT configurations, not just custom ones

### 2. The Managed Service Abstraction

**Problem:** Assuming managed services work like self-hosted
```
Self-hosted Redis: Full control over redis.conf
ElastiCache: Many settings managed internally
```

**Lesson:** Managed services are opinionated; respect their constraints

### 3. The Cost Surprise

**Problem:** Production-safe defaults that surprise with costs
```json
{
  "replicasPerNodeGroup": 2,  // 3x the cost!
  "multiAzEnabled": true       // Additional charges
}
```

**Lesson:** Dev-friendly defaults with production guidance in descriptions

### 4. The Version Dependency

**Problem:** Features requiring specific versions
```json
{
  "ipDiscovery": "ipv6"  // Requires Redis 6.2+
}
```

**Lesson:** Document version requirements clearly

---

## Documentation and README Generation

### Auto-Generated Documentation

Components use `readme-generator.sh` to create documentation from schemas:

```bash
#!/bin/bash
# Run from component root after any schema changes
bash readme-generator.sh
```

### Documentation Structure

```
Component/
├── README.md.tpl         # Template with static content
├── README.md            # Generated (do not edit directly)
├── schema.json          # Source of property tables
├── defaults.json        # Source of default values
└── <flavour>/
    ├── README.md.tpl    # Flavour-specific template
    └── README.md        # Generated flavour docs
```

### README Template Best Practices

1. **Component Overview**: Clear explanation of what the component does
2. **Quick Start**: Minimal configuration example
3. **Property Tables**: Auto-generated from schemas
4. **Production Guide**: Security and scaling recommendations
5. **Examples**: Common configuration patterns
6. **Troubleshooting**: Known issues and solutions

### Property Table Generation

The generator creates tables from schema:
- Property name and type
- Required/Optional status
- Default values from defaults.json
- Descriptions from schema
- Validation constraints

### Maintaining Documentation

1. **Never edit README.md directly** - Edit templates and schemas
2. **Run generator after schema changes** - Keep docs in sync
3. **Update templates for structural changes** - Add new sections as needed
4. **Include examples** - Show real-world usage patterns

---

## Appendix: Real-World Example - Redis Component Analysis

### Properties That Failed LSP Testing

| Property | Why It Failed | Lesson |
|----------|--------------|--------|
| `port` | ElastiCache fixes to 6379 | Managed services abstract infrastructure |
| `persistenceMode` | Not configurable in ElastiCache | Cloud providers manage persistence internally |
| `databases` | Not documented as configurable | Don't assume all Redis features are exposed |
| `notifyKeyspaceEvents` | Requires custom parameter group | Hidden dependencies break LSP |

### Properties Successfully Placed

| Property | Location | Why |
|----------|----------|-----|
| `redisVersion` | Root | Fundamental to client compatibility |
| `clusterModeEnabled` | Root | Changes connection model |
| `replicationGroupId` | AWS Flavour | AWS-specific identifier |
| `cacheNodeType` | AWS Flavour | Platform-specific resource types |
