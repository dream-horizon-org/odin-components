import com.dream11.Odin

Odin.component {
    dslVersion "2.5.0"
    flavour {
        name "aws_flavour"
        deploy {
            String lastState = getLastState()
            run "bash operate.sh deploy"
            discovery {
                run "echo '{}'"
            }
        }

        healthcheck {
            linearRetryPolicy {
                count 50
                intervalSeconds 3
            }
            script{
                filePath "./healthcheck.sh"
            }
        }

        undeploy {
            run "bash operate.sh undeploy"
        }
    }
}
