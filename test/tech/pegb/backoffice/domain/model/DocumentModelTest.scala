package tech.pegb.backoffice.domain.model

import java.time.LocalDateTime
import java.util.UUID

import org.scalatest.Ignore
import org.scalatestplus.play.PlaySpec
import tech.pegb.backoffice.domain.document.model.{Document, DocumentStatuses, DocumentTypes}
import tech.pegb.backoffice.util.Utils

@Ignore
class DocumentModelTest extends PlaySpec {

  "Document model" should {
    "not instantiate if purpose is empty" in {
      val caught = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = null,
          status = DocumentStatuses.fromString(Document.Pending),
          rejectionReason = None,
          checkedBy = None,
          checkedAt = None,
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught.getMessage == "assertion failed: empty purpose")
    }

    "not instantiate if status is PENDING but checkedBy or checkedAt has value" in {
      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Pending),
          rejectionReason = None,
          checkedBy = Option("ninja user"),
          checkedAt = Some(Utils.nowAsLocal()),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught1.getMessage == "assertion failed: checkedBy and/or checkedAt cannot have value if status is pending")

      val caught2 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Pending),
          rejectionReason = None,
          checkedBy = None,
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught2.getMessage == "assertion failed: checkedBy and/or checkedAt cannot have value if status is pending")
    }

    "not instantiate if status is REJECTED but rejectionReason is empty" in {
      val caught = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Rejected),
          rejectionReason = None,
          checkedBy = None,
          checkedAt = None,
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught.getMessage == "assertion failed: empty rejection reason")
    }

    "not instantiate if status is not REJECTED but has rejectionReason" in {
      val caught = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Approved),
          rejectionReason = Option("this is rejected not approved"),
          checkedBy = Option("stupid user"),
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught.getMessage == "assertion failed: has rejection reason but status is not rejected")
    }

    "not instantiate if status is REJECTED but checkedBy has no value" in {
      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Rejected),
          rejectionReason = Option("fake document"),
          checkedBy = Option(""),
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught1.getMessage == "assertion failed: checkedBy required for status [rejected]")

      val caught2 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Rejected),
          rejectionReason = Option("fake document"),
          checkedBy = None,
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught2.getMessage == "assertion failed: checkedBy required for status [rejected]")
    }

    "not instantiate if status is APPROVED but checkedBy has no value" in {
      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Approved),
          rejectionReason = None,
          checkedBy = Option(""),
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught1.getMessage == "assertion failed: checkedBy required for status [approved]")

      val caught2 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Approved),
          rejectionReason = None,
          checkedBy = None,
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught2.getMessage == "assertion failed: checkedBy required for status [approved]")
    }

    "not instantiate if status is REJECTED but checkedAt has no value" in {
      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Rejected),
          rejectionReason = Some("fake document"),
          checkedBy = Option("some user"),
          checkedAt = None,
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught1.getMessage == "assertion failed: checkedAt required for status [rejected]")
    }

    "not instantiate if status is APPROVED but checkedAt has no value" in {
      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Approved),
          rejectionReason = None,
          checkedBy = Option("some user"),
          checkedAt = None,
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught1.getMessage == "assertion failed: checkedAt required for status [approved]")
    }

    "not instantiate if uploadedBy has value but uploadedAt is empty or vice versa" in {

      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Pending),
          rejectionReason = None,
          checkedBy = None,
          checkedAt = None,
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = Option("ninja uploader"),
          updatedAt = None)
      }
      assert(caught1.getMessage == "assertion failed: empty uploadedBy and/or uploadedAt")

      val caught2 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Pending),
          rejectionReason = None,
          checkedBy = None,
          checkedAt = None,
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = Option(LocalDateTime.now),
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught2.getMessage == "assertion failed: empty uploadedBy and/or uploadedAt")
    }

    "not instantiate if status is APPROVED but uploadedBy and/or uploadedAt has no value" in {
      val caught1 = intercept[AssertionError] {
        Document(
          id = UUID.randomUUID(),
          customerId = Some(UUID.randomUUID()),
          applicationId = Some(UUID.randomUUID()),
          documentName = None,
          documentType = DocumentTypes.fromString("national_id"),
          documentIdentifier = None,
          purpose = "unit testing",
          status = DocumentStatuses.fromString(Document.Approved),
          rejectionReason = None,
          checkedBy = Option("some user"),
          checkedAt = Option(LocalDateTime.now),
          createdBy = "some user",
          createdAt = LocalDateTime.now,
          fileUploadedAt = None,
          fileUploadedBy = None,
          updatedAt = None)
      }
      assert(caught1.getMessage == s"assertion failed: status cannot be approved if uploadedBy and uploadedAt is empty")
    }
  }
}
